(ns clj.zots.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [io.pedestal.interceptor.chain :as ic-chain]
            [io.pedestal.http.content-negotiation :as contneg]
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
     :path "/"
     :domain "localhost"
     :expires exp-date-str}}))

(defn response-with-cookies
 [status body cookies & {:as headers}]
 (cooks/cookies-response
  {:status status
   :body body
   :cookies cookies
   :headers headers}))

(defonce database (atom {}))

(defn find-game-by-id
 [dbval id]
 (get dbval id))

(def db-interceptor
 {:name :database-interceptor
  :enter
  (fn [context]
   (println "db-inter")
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
    (println "game-view-edn inter")
    (let [game (get-in context [:request :game])
          accepted (get-in context [:request :accept :field] "application/edn")]
      (assoc context :response (ok game "Content-Type" accepted))))})

(def game-view-html
 {:name :game-view-html
  :enter
  (fn [context]
    (println "game-view-html inter")
    (let [page (-> (io/resource "public/index.html") slurp)
          accepted (get-in context [:request :accept :field] "application/edn")]
      (assoc context
        :response
        (ok page "Content-Type" accepted))))})

(def game-view
 {:name :game-view
  :enter
  (fn [context]
   (println "game-view inter")
   (let [accepted (get-in context [:request :accept :field] "application/edn")]
     (println accepted)
     (case accepted
       "text/html" (ic-chain/enqueue context [game-view-html])
       "application/edn" (ic-chain/enqueue context [game-view-edn]))))})

(def game-db-check
 {:name :game-db-check
  :enter
  (fn [context]
   (println "game-db-check inter")
   (if-let [db-id (get-in context [:request :path-params :game-id])]
     (if-let [game (find-game-by-id (get-in context [:request :database]) db-id)]
       (assoc-in context [:request :game] game)
       (ic-chain/terminate (assoc context :response (not-found "Game not found"))))
     (ic-chain/terminate (assoc context :response (not-found "Game not found")))))})


(def move-check
 {:name :move-check
  :enter
  (fn [context]
    (let [move (get-in context [:request :edn-params])
          game (get-in context [:request :game])]
      (if (game/valid-move? game move)
        context
        (ic-chain/terminate
         (assoc context :response (bad-request "Invalid move"))))))})



(def game-create
 {:name :game-create
  :enter
  (fn [context]
   (let [db-id (:game-id context)
         new-game (-> (game/new-game) pr-str)
         url (route/url-for :game-view :params {:game-id db-id})
         cookie (get context :cookie)]
     (assoc context
            :response (response-with-cookies 201 new-game cookie "Location" url)
            :tx-data [assoc db-id new-game])))})

(def game-update
 {:name :game-update
  :enter
  (fn [context]
   (let [db-id (get-in context [:request :path-params :game-id])
         move (get-in context [:request :edn-params])
         game (get-in context [:request :game])
         next-game (game/make-move game move)]
    (assoc context
           :response (ok next-game)
           :tx-data [assoc db-id next-game])))})



(def supported-types ["text/html" "application/edn"])

(def negotiate-content (contneg/negotiate-content supported-types))

(def get-game-as-content
 {:name :get-game-as-content
  :enter
  (fn [context]
   (let [accepted (get-in context [:request :accept :field] "application/edn")]
     (if (= "text/html" accepted)
       (let [page (-> (io/resource "public/index.html") slurp)
             cookie (:cookie context)]
         (ic-chain/terminate
          (assoc context
            :response
            (response-with-cookies 200 page cookie "Content-Type" accepted))))
       context)))})

(def stamp-init-cookie
 {:name :stamp-init-cookie
  :enter
  (fn [context]
    (let [id (:game-id context)
          cookie (generate-player-cookie id "red")]
      (assoc context :cookie cookie)))})

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
  get-game-as-content
  db-interceptor
  game-create])

(def get-game-by-id-interceptors
 [negotiate-content
  db-interceptor
  game-db-check
  game-view])

(def post-game-interceptors
 [(body-params/body-params)
  db-interceptor
  game-db-check
  move-check
  game-update])

(def routes
 (route/expand-routes
  #{["/game"          :get  get-game-interceptors]
    ["/game/:game-id" :get  get-game-by-id-interceptors]
    ["/game/:game-id" :post post-game-interceptors]}))

(def service-map
 {::http/routes routes
  ::http/type   :jetty
  ::http/port   8890
  ::http/resource-path "/public"
  ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})

(defn start []
 (http/start (http/create-server service-map)))

(defonce server (atom nil))

(defn start-dev []
 (reset! server
         (http/start (http/create-server
                       (assoc service-map
                              ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(defn test-request
 [verb url]
 (test/response-for (::http/service-fn @server) verb url))
