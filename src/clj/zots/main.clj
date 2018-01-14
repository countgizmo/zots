(ns clj.zots.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [io.pedestal.interceptor.chain :as ic-chain]
            [cljc.zots.game :as game]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [ring.middleware.cookies :as cooks]))

(defn response
 [status body & {:as headers}]
 {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))
(def updated (partial response 204))
(def bad-request (partial response 400))
(def not-found (partial response 404))

(def cookie-exp-date-formatter (f/formatter "EEE, dd MMM yyyy HH:mm:ss"))

(defn generate-player-cookie
 [game-id player]
 (let [key (str "player-" game-id)
       today-noon (t/today-at 12 00 00)
       exp-date (t/plus today-noon (t/months 6))
       exp-date-str (-> (f/unparse cookie-exp-date-formatter exp-date)
                        (str " GMT"))]
   {key
    {:value player,
     :secure true,
     :max-age (t/interval today-noon exp-date)
     :expires exp-date-str}}))

(defn response-with-cookies
 [status body cookies & {:as headers}]
 (cooks/cookies-response
  {:status status
   :body body
   :cookies cookies
   :headers headers}))

(defn game-created-response
 [id body url]
 (let [cookie (generate-player-cookie id "red")]
   (response-with-cookies 201 body cookie "Location" url)))

(defonce database (atom {}))

(defn find-game-by-id
 [dbval id]
 (get dbval id))

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

(def result-check
 {:name :entity-render
  :leave
  (fn [context]
   (if-let [result (:result context)]
     (assoc context :response (ok result))
     context))})

(def game-view
 {:name :game-view
  :enter
  (fn [context]
   (if-let [db-id (get-in context [:request :path-params :game-id])]
     (if-let [game (find-game-by-id (get-in context [:request :database]) db-id)]
       (assoc context :result game)
       context)
     context))})

(def game-db-check
 {:name :game-db-check
  :enter
  (fn [context]
   (if-let [db-id (get-in context [:request :path-params :game-id])]
     (if-let [game (find-game-by-id (get-in context [:request :database]) db-id)]
       (assoc-in context [:request :game] game)
       (ic-chain/terminate (assoc context :response (not-found "Game not found"))))
     (ic-chain/terminate(assoc context :response (not-found "Game not found")))))})


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
   (let [db-id (str (gensym "1"))
         new-game (game/new-game)
         url (route/url-for :game-view :params {:game-id db-id})]
     (assoc context
            :response (game-created-response db-id new-game url)
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

(def post-game-interceptors
 [(body-params/body-params)
  db-interceptor
  game-db-check
  move-check
  game-update])

(def routes
 (route/expand-routes
  #{["/game"          :get  [db-interceptor game-create]]
    ["/game/:game-id" :get  [result-check db-interceptor game-view]]
    ["/game/:game-id" :post post-game-interceptors]}))

(def service-map
 {::http/routes routes
  ::http/type   :jetty
  ::http/port   8890})

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
