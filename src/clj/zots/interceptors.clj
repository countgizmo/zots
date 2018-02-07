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
            [clojure.java.io :as io]))

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
 (let [key (str "player_" game-id)
       today-noon (t/today-at 12 00 00)
       exp-date (t/plus today-noon (t/months 6))
       exp-date-str (-> (f/unparse cookie-exp-date-formatter exp-date)
                        (str " GMT"))]
   {key
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
 (get dbval id))

(defn cookie-key
 [id]
 (str "player_" id))

(defn cookie-for-game-exists?
 [cookies id]
 (contains? cookies (cookie-key id)))

(defn get-player-from-cookie
 ([cookies id]
  (get-player-from-cookie cookies id nil))
 ([cookies id fallback]
  (get-in cookies [(cookie-key id) :value] fallback)))

(def db-interceptor
 {:name :database-interceptor
  :enter
  (fn [context]
   (update context :request assoc :database @database))
  :leave
  (fn [context]
    (if-let [[op & args] (:tx-data context)]
      (do
        (apply swap! database op args)
        (assoc-in context [:request :database] @database))
      context))})

(def game-view-edn
 {:name :game-view-edn
  :enter
  (fn [context]
    (let [game (get-in context [:request :game-data :game])
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
        (ok page "Content-Type" accepted))))})

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
     (if-let [game-data
              (find-game-data-by-id (get-in context [:request :database]) db-id)]
       (assoc-in context [:request :game-data] game-data)
       (ic-chain/terminate (assoc context :response (not-found "Game not found"))))
     (ic-chain/terminate (assoc context :response (not-found "Game not found")))))})


(def move-check
 {:name :move-check
  :enter
  (fn [context]
    (let [move (get-in context [:request :edn-params])
          game (get-in context [:request :game-data :game])]
      (if (game/valid-move? game move)
        context
        (ic-chain/terminate
         (assoc context :response (bad-request "Invalid move"))))))})


(def cookie-turn-check
 {:name :cookie-turn-check
  :enter
  (fn [context]
    (let [cookies (get-in context [:request :cookies])
          game-id (get-in context [:request :path-params :game-id])
          player (get-player-from-cookie cookies game-id)
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
   (let [db-id (:game-id context)
         new-game (game/new-game)
         url (route/url-for :fetch-game :params {:game-id db-id})
         cookie (get context :cookie)
         accepted (get-in context [:request :accept :field] "application/edn")
         status (if (= accepted "text/html") 303 201)]
     (assoc context
            :response (response-with-cookies
                        status
                        (pr-str new-game)
                        cookie
                        "Location" url
                        "Content-Type" accepted)
            :tx-data [assoc-in [db-id :game] new-game])))})

(def game-update
 {:name :game-update
  :enter
  (fn [context]
   (let [db-id (get-in context [:request :path-params :game-id])
         move (get-in context [:request :edn-params])
         game (get-in context [:request :game-data :game])
         next-game (game/make-move game move)]
    (assoc context
           :response (ok next-game)
           :tx-data [assoc-in [db-id :game] next-game])))})

(def supported-types ["text/html" "application/edn"])

(def negotiate-content (contneg/negotiate-content supported-types))

(def stamp-init-cookie
 {:name :stamp-init-cookie
  :enter
  (fn [context]
    (let [id (:game-id context)
          cookie (generate-player-cookie id "red")]
      (assoc context :cookie cookie)))})

(defn determine-player-fallback
 [slots]
 (cond
  (= #{:red :blue} slots) "none"
  :else "blue"))

(def cookie-check
 {:name :cookie-check
  :enter
  (fn [context]
    (let [cookies (get-in context [:request :cookies])
          game-id (get-in context [:request :path-params :game-id])
          slots (get-in context [:request :game-data :slots])
          player (get-player-from-cookie cookies game-id (determine-player-fallback slots))
          cookie (generate-player-cookie game-id player)]
        (update-in context [:request :cookies] conj cookie)))
  :leave
  (fn [context]
    (let [cookies (get-in context [:request :cookies])]
      (assoc-in context [:response :cookies] cookies)))})

(def check-slots
 {:name :cookie-check
  :leave
  (fn [context]
    (let [slots (get-in context [:request :game-data :slots] #{})
          db-id (get-in context [:request :path-params :game-id])
          cookies (get-in context [:request :cookies])
          player (-> (get-player-from-cookie cookies db-id) keyword)]
      (cond
       (or (player slots) (= #{:red :blue} slots)) context
       (empty? slots)
       (assoc context :tx-data [assoc-in [db-id :slots] #{player}])
       :else
       (assoc context
        :tx-data [update-in [db-id :slots] conj player]))))})

(def generate-game-id
 {:name :generate-game-id
  :enter
  (fn [context]
    (let [id (str (gensym "1"))]
      (assoc context :game-id id)))})

(def get-game-interceptors
 [negotiate-content
  generate-game-id
  stamp-init-cookie
  db-interceptor
  game-create])

(def get-game-by-id-interceptors
 [ring-mid/cookies
  negotiate-content
  db-interceptor
  game-db-check
  cookie-check
  game-view
  check-slots])

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
