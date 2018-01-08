(ns clj.zots.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [cljc.zots.board :as board]))

(defn response
 [status body & {:as headers}]
 {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))
(def updated (partial response 204))
(def validation-failed (partial response 400))

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

(def game-create
 {:name :game-create
  :enter
  (fn [context]
   (let [db-id (str (gensym "1"))
         new-board (board/gen-empty-board)
         new-game {:board new-board :turn :red}
         url (route/url-for :game-view :params {:game-id db-id})]
     (assoc context
            :response (created new-board "Location" url)
            :tx-data [assoc db-id new-board])))})

(def game-update
 {:name :game-update
  :enter
  (fn [context]
   (let [db-id (get-in context [:request :path-params :game-id])
         body (get-in context [:request :edn-params])
         player (:player body)]
    (assoc context
           :response (ok {:turn player}))))})

(def routes
 (route/expand-routes
  #{["/game"          :get  [db-interceptor game-create]]
    ["/game/:game-id" :get  [result-check db-interceptor game-view]]
    ["/game/:game-id" :post [(body-params/body-params) game-update]]}))

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
