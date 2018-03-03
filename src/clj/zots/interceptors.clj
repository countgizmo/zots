(ns clj.zots.interceptors
  (:require [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor.chain :as ic-chain]
            [io.pedestal.http.content-negotiation :as contneg]
            [io.pedestal.http.ring-middlewares :as ring-mid]
            [cljc.zots.game :as game]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [ring.middleware.cookies :as cooks]
            [clojure.java.io :as io]
            [datomic.client.api :as d]))

(defn response
 [status body & {:as headers}]
 {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))
(def updated (partial response 204))
(def bad-request (partial response 400))
(def not-found (partial response 404))

(def cookie-exp-date-formatter (f/formatter "EEE, dd-MMM-yyyy HH:mm:ss"))

(defn generate-player-cookie
 [game-id player]
 (let [today-noon (t/today-at 12 00 00)
       exp-date (t/plus today-noon (t/months 6))
       exp-date-str (-> (f/unparse cookie-exp-date-formatter exp-date)
                        (str " GMT"))]
   {"player"
    {:value player
     :path (str "/game/" game-id)
     :expires exp-date-str}}))

(defn response-with-cookies
 [status body cookies & {:as headers}]
 (cooks/cookies-response
  {:status status
   :body body
   :cookies cookies
   :headers headers}))

(defonce database (atom {}))

(defn find-game-data-by-id
 [dbval id]
 (get dbval (str id)))

(defn get-player-from-cookie
 ([cookie]
  (get-player-from-cookie cookie nil))
 ([cookie fallback]
  (if-let [player (get-in cookie ["player" :value])]
    player
    fallback)))

(def db-interceptor
 {:name :database-interceptor
  :enter
  (fn [context]
   (assoc context :database @database))
  :leave
  (fn [context]
    (if-let [[op & args] (:tx-data context)]
      (do
        (apply swap! database op args)
        (assoc context :database @database))
      context))})

(def game-view-edn
 {:name :game-view-edn
  :enter
  (fn [context]
    (let [game (get-in context [:game-data])
          accepted (get-in context [:request :accept :field] "application/edn")]
      (assoc context :response (ok game "Content-Type" accepted))))})

(def game-view-html
 {:name :game-view-html
  :enter
  (fn [context]
    (let [page (-> (io/resource "public/index.html") slurp)
          accepted (get-in context [:request :accept :field] "application/edn")]
      (assoc context
        :response
        (ok page
          "Content-Type" accepted
          "Cache-Control" "no-store"
          "Cache-Control" "no-cache, no-store, must-revalidate"))))})

(def game-view
 {:name :game-view
  :enter
  (fn [context]
   (let [accepted (get-in context [:request :accept :field] "application/edn")]
     (case accepted
       "text/html" (ic-chain/enqueue context [game-view-html])
       "application/edn" (ic-chain/enqueue context [game-view-edn]))))})

(def game-db-check
  {:name :game-db-check
   :enter
   (fn [context]
    (if-let [db-id (get-in context [:request :path-params :game-id])]
      (if-let [game-data (find-game-data-by-id (:database context) db-id)]
       (assoc context :game-data game-data)
       (ic-chain/terminate (assoc context :response (not-found "Game not found"))))
      (ic-chain/terminate (assoc context :response (not-found "Game not found")))))})

(def move-check
 {:name :move-check
  :enter
  (fn [context]
    (let [move (get-in context [:request :edn-params])
          game (get-in context [:game-data])]
      (if (game/valid-move? game move)
        context
        (ic-chain/terminate
         (assoc context :response (bad-request "Invalid move"))))))})

(def cookie-turn-check
 {:name :cookie-turn-check
  :enter
  (fn [context]
    (let [cookies (:cookies context)
          game-id (get-in context [:request :path-params :game-id])
          player (get-player-from-cookie cookies)
          turn (get-in context [:request :edn-params :turn])]
      (if (or
            (nil? player)
            (not= (name turn) (name player)))
        (ic-chain/terminate
         (assoc context :response (bad-request "Invalid player")))
        context)))})

(def game-create
 {:name :game-create
  :enter
  (fn [context]
   (let [game-id (:game-id context)
         game-data (:game-data context)
         url (route/url-for :fetch-game :params {:game-id game-id})
         cookie (:cookie context)
         accepted (get-in context [:request :accept :field] "application/edn")
         status (if (= accepted "text/html") 303 201)]
     (assoc context
            :response (response-with-cookies
                        status
                        (pr-str game-data)
                        cookie
                        "Location" url
                        "Content-Type" accepted)
            :tx-data [assoc game-id game-data])))})

(def game-update
 {:name :game-update
  :enter
  (fn [context]
   (let [db-id (get-in context [:request :path-params :game-id])
         move (get-in context [:request :edn-params])
         game (get-in context [:game-data])
         next-game (game/make-move game move)]
    (assoc context
           :response (ok next-game)
           :tx-data [assoc db-id next-game])))})

(def supported-types ["text/html" "application/edn"])

(def negotiate-content (contneg/negotiate-content supported-types))

(def stamp-init-cookie
 {:name :stamp-init-cookie
  :enter
  (fn [context]
    (let [id (:game-id context)
          cookie (generate-player-cookie id "red")]
      (assoc context :cookie cookie)))})

(def update-slots
  {:name :update-slots
   :enter
   (fn [context]
     (let [game-id (get-in context [:request :path-params :game-id])
           cookies (:cookies context)
           player (get-player-from-cookie cookies)
           slots (get-in context [:game-data :slots] #{})
           new-slots (conj slots (keyword player))
           next-game (assoc (:game-data context) :slots new-slots)]
       (if (= new-slots slots)
         context
         (assoc context :tx-data [assoc game-id next-game]))))})

(def cookie-check
 {:name :cookie-check
  :enter
  (fn [context]
    (let [cookies (get-in context [:request :cookies])
          game-id (get-in context [:request :path-params :game-id])
          player (get-player-from-cookie cookies "none")
          cookie (generate-player-cookie game-id player)]
      (assoc context :cookies cookie)))
  :leave
  (fn [context]
    (let [cookies (:cookies context)]
      (assoc-in context [:response :cookies] cookies)))})

(def generate-game-id
 {:name :generate-game-id
  :enter
  (fn [context]
    (let [prefix (str (rand-int 9999999))
          id (str (gensym prefix))]
      (assoc context :game-id id)))})

(def create-new-game
  {:name :create-new-game
   :enter
   (fn [context]
     (let [new-game (game/new-game)]
       (assoc context :game-data new-game)))})

(def populate-red-slot
  {:name :populate-red-slot
   :enter
   (fn [context]
     (if-let [game-data (:game-data context)]
       (assoc-in context [:game-data :slots] #{:red})
       context))})

(def get-game-interceptors
  [negotiate-content
   create-new-game
   generate-game-id
   db-interceptor
   populate-red-slot
   stamp-init-cookie
   game-create])

(def get-game-by-id-interceptors
  [ring-mid/cookies
   negotiate-content
   db-interceptor
   game-db-check
   cookie-check
   update-slots
   game-view])

(def post-game-interceptors
  [ring-mid/cookies
   negotiate-content
   (body-params/body-params)
   db-interceptor
   game-db-check
   cookie-check
   cookie-turn-check
   move-check
   game-update])

; (def cfg {:server-type :peer-server
;           :access-key "myaccesskey"
;           :secret "mysecret"
;           :endpoint "localhost:8998"})
;
; (def client (d/client cfg))
; (def conn (d/connect client {:db-name "hello"}))
;
;
; (d/transact conn {:tx-data schema})
;
; (def add-cells
;   {:game/id 1
;    :game/cells
;     [{:coord/x 1 :coord/y 1 :cell/surrounded? true :cell/player :red :cell/status :active}
;      {:coord/x 0 :coord/y 0 :cell/surrounded? false :cell/player :blue :cell/status :wall}
;      {:coord/x 2 :coord/y 2 :cell/surrounded? false :cell/player :none :cell/status :active}]})
;
; (d/transact conn {:tx-data [add-cells]})
;
; (def db (d/db conn))
;
; (d/q
;   '[:find ?x ?y ?player ?status ?surrounded
;     :where [?e :game/id ?id]
;            [?e :game/cells ?cell]
;            [?cell :cell/player ?pl-ref]
;            [?pl-ref _ ?player]
;            [?cell :coord/x ?x]
;            [?cell :coord/y ?y]
;            [?cell :cell/surrounded? ?surrounded]
;            [?cell :cell/status ?st-ref]
;            [?st-ref _ ?status]]
;   db)
