(ns clj.zots.routes
  (:require [io.pedestal.http.route :as route]
            [clj.zots.interceptors :as ic]
            [io.pedestal.http :as http]))

(defn get-all-routes
  [db-connection]
  (let [{:keys [get-game get-game-by-id post-game]}
        (ic/get-all-interceptors db-connection)]
    (route/expand-routes
      #{["/game"          :get  get-game :route-name :create-game]
        ["/game/:game-id" :get  get-game-by-id :route-name :fetch-game]
        ["/game/:game-id" :post post-game :route-name :update-game]})))
