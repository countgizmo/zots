(ns clj.zots.main
  (:gen-class)
  (:require [io.pedestal.http :as http]
            [clj.zots.system :as system]))

(defonce server (atom nil))

(defn start-dev []
  (reset! server
    (http/start (:server/http-dev (system/start-dev)))))

(defn restart []
  (http/stop @server)
  (start-dev))

(defn -main [& args]
  (system/start))
