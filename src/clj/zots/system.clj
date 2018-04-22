(ns clj.zots.system
  (:require [integrant.core :as ig]
            [datomic.client.api :as d]
            [clj.zots.routes :as routes]
            [io.pedestal.http :as http]))

(defn config
  []
  (ig/read-string (slurp "resources/system.edn")))

(defmethod ig/init-key :datomic/connection
  [_ {:keys [db-name client]}]
  (let [client (d/client client)]
    (d/connect client {:db-name db-name})))

(defmethod ig/init-key :pedestal/routes
  [_ {:keys [db-connection]}]
  (routes/get-all-routes db-connection))

(defmethod ig/init-key :server/http
  [_ server-settings]
  (http/start (http/create-server server-settings)))

(defmethod ig/init-key :server/http-dev
  [_ server-settings]
  (http/create-server server-settings))

(defn start
  []
  (ig/init (config)))

(defn start-dev
  []
  (ig/init (config) [:server/http-dev]))
