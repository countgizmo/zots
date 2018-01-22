(ns clj.zots.service
  (:require [io.pedestal.http.route :as route]
            [clj.zots.interceptors :as ic]
            [io.pedestal.http :as http]))
(def routes
 (route/expand-routes
  #{["/game"          :get  ic/get-game-interceptors]
    ["/game/:game-id" :get  ic/get-game-by-id-interceptors]
    ["/game/:game-id" :post ic/post-game-interceptors]}))

(def service-map
 {::http/routes routes
  ::http/type   :jetty
  ::http/port   8890
  ::http/resource-path "/public"
  ::http/secure-headers {:content-security-policy-settings {:object-src "'none'"}}})
