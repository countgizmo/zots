(ns clj.zots.main
  (:gen-class)
  (:require [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [clj.zots.service :as service]))

(defn start []
  (http/start (http/create-server service/service-map)))

(defonce server (atom nil))

(defn start-dev []
  (reset! server
    (http/start
      (http/create-server
        (assoc service/service-map
               ::http/join? false)))))

(defn stop-dev []
  (http/stop @server))

(defn restart []
  (stop-dev)
  (start-dev))

(defn -main [& args]
  (start))
