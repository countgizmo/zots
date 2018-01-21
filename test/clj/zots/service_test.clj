(ns clj.zots.service-test
 (:require [clojure.test :refer :all]
           [io.pedestal.test :refer :all]
           [io.pedestal.http :as http]
           [clojure.spec.alpha :as s]
           [ring.middleware.cookies :as cookies]
           [cljc.zots.specs :as specs]
           [clj-time.format :as f]
           [clj-time.core :as t]
           [clj.zots.service :as service]))

(def service
  (::http/service-fn (http/create-servlet service/service-map)))

(def formatter (f/formatter "EEE, dd-MMM-yyyy HH:mm:ss ZZZ"))

(defn get-expires
 [raw-cookie]
 (-> (re-matches #".*Expires=(.*)" raw-cookie) last))

(deftest get-initial-page-as-html
 (let [headers {"Accept" "text/html"}
       response (response-for service :get "/game" :headers headers)
       raw-cookie (first (get-in response [:headers "Set-Cookie"]))
       first-day (t/today-at 12 00 00)
       last-day (t/plus first-day (t/months 6))
       exp-day (f/parse formatter (get-expires raw-cookie))]
  (is (zero? (t/in-minutes (t/interval last-day exp-day))))
  (is (= 303 (:status response)))
  (is (not (empty? (get-in response [:headers "Location"]))))
  (is (re-find #"red" raw-cookie))
  (is (= "text/html" (get-in response [:headers "Content-Type"])))))

(deftest get-initial-page-as-edn
 (let [headers {"Accept" "application/edn"}
       response (response-for service :get "/game" :headers headers)
       raw-cookie (first (get-in response [:headers "Set-Cookie"]))]
  (is (= 201 (:status response)))
  (is (= "application/edn" (get-in response [:headers "Content-Type"])))
  (is (not (empty? (get-in response [:headers "Location"]))))
  (is (re-find #"red" raw-cookie))
  (is (s/valid? :specs/game (read-string (:body response))))))


(def cs "player_150992=red;Path=/game/150992;Domain=localhost;Expires=Sat, 21-Jul-2018 12:00:00 GMT")
