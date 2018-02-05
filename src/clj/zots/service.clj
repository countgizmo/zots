(ns clj.zots.service
  (:require [io.pedestal.http.route :as route]
            [clj.zots.interceptors :as ic]
            [io.pedestal.http :as http]))
(def routes
 (route/expand-routes
  #{["/game"          :get  ic/get-game-interceptors :route-name :create-game]
    ["/game/:game-id" :get  ic/get-game-by-id-interceptors :route-name :fetch-game]
    ["/game/:game-id" :post ic/post-game-interceptors :route-name :update-game]}))

(def service-map
 {::http/routes routes
  ::http/type   :jetty
  ::http/port   8890
  ::http/resource-path "/public"
  ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})
