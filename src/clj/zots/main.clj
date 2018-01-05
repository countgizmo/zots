(ns clj.zots.main
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.test :as test]
            [cljc.zots.board :as board]))

(defn response
 [status body & {:as headers}]
 {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))

(def echo
  {:name :echo
   :enter
   (fn [context]
    (let [request (:request context)
          response (ok context)]
      (assoc context :response response)))})

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
 {:name :board-create
  :enter
  (fn [context]
   (let [db-id (str (gensym "1"))
         new-board (board/gen-empty-board)
         url (route/url-for :game-view :params {:game-id db-id})]
     (assoc context
            :response (created new-board "Location" url)
            :tx-data [assoc db-id new-board])))})

(def routes
 (route/expand-routes
  #{["/game"          :get  [db-interceptor game-create]]
    ["/game/:game-id" :get  [db-interceptor game-view]]}))

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
