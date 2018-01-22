(ns clj.zots.service-test
 (:require [clojure.test :refer :all]
           [io.pedestal.test :refer :all]
           [io.pedestal.http :as http]
           [clojure.spec.alpha :as s]
           [ring.middleware.cookies :as cookies]
           [cljc.zots.specs :as specs]
           [clj-time.format :as f]
           [clj-time.core :as t]
           [clj.zots.service :as service]
           [clj.zots.interceptors :as interceptors]
           [cljc.zots.game :as game]))

(def cookie-example "player_150992=red;Path=/game/150992;Domain=localhost;Expires=Sat, 21-Jul-2018 12:00:00 GMT")

(def service
  (::http/service-fn (http/create-servlet service/service-map)))

(def formatter (f/formatter "EEE, dd-MMM-yyyy HH:mm:ss ZZZ"))

(defn get-expires
 [raw-cookie]
 (-> (re-matches #".*Expires=(.*)" raw-cookie) last))

(defn check-common-init-response
 [response]
 (let [raw-cookie (first (get-in response [:headers "Set-Cookie"]))
       first-day (t/today-at 12 00 00)
       last-day (t/plus first-day (t/months 6))
       exp-day (f/parse formatter (get-expires raw-cookie))]
  (is (zero? (t/in-minutes (t/interval last-day exp-day))))
  (is (not (empty? (get-in response [:headers "Location"]))))
  (is (re-find #"red" raw-cookie))
  (is (s/valid? :specs/game (read-string (:body response))))))

(deftest get-initial-page-as-html
 (let [headers {"Accept" "text/html"}
       response (response-for service :get "/game" :headers headers)]
  (is (= 303 (:status response)))
  (is (= "text/html" (get-in response [:headers "Content-Type"])))
  (check-common-init-response response)))

(deftest get-initial-page-as-edn
 (let [headers {"Accept" "application/edn"}
       response (response-for service :get "/game" :headers headers)]
  (is (= 201 (:status response)))
  (is (= "application/edn" (get-in response [:headers "Content-Type"])))
  (check-common-init-response response)))

(defonce mocked-db
 (atom {"1" (game/new-game)}))

(deftest get-existing-page-as-html-no-cookie
 (with-redefs [interceptors/database mocked-db]
   (let [headers {"Accept" "text/html"}
         response (response-for service :get "/game/1" :headers headers)
         raw-cookie (first (get-in response [:headers "Set-Cookie"]))]
     (is (= 200 (:status response)))
     (is (re-find #"blue" raw-cookie)))))

(deftest get-existing-page-as-edn-with-cookie
 (with-redefs [interceptors/database mocked-db]
   (let [headers {"Accept" "application/edn" "Cookie" "player_1=red"}
         response (response-for service :get "/game/1" :headers headers)
         raw-cookie (first (get-in response [:headers "Set-Cookie"]))]
     (is (= 200 (:status response)))
     (is (s/valid? :specs/game (read-string (:body response))))
     (is (re-find #"red" raw-cookie)))))

(deftest get-non-existing-page-as-html
 (with-redefs [interceptors/database mocked-db]
   (let [headers {"Accept" "text/html"}
         response (response-for service :get "/game/0" :headers headers)
         raw-cookie (first (get-in response [:headers "Set-Cookie"]))]
     (is (= 404 (:status response)))
     (is (empty? raw-cookie)))))

(deftest post-non-existing-game
  (with-redefs [interceptors/database mocked-db]
   (let [headers {"Accept" "application/edn"}
         response (response-for service :post "/game/0" :headers headers)
         raw-cookie (first (get-in response [:headers "Set-Cookie"]))]
     (is (= 404 (:status response)))
     (is (empty? raw-cookie)))))

(deftest post-valid-move-with-no-cookie
  (with-redefs [interceptors/database mocked-db]
   (swap! mocked-db assoc-in ["1" :turn] :red)
   (let [headers {"Accept" "application/edn" "Content-Type" "application/edn"}
         move "{:turn :red :x 0 :y 0}"
         response (response-for service
                    :post "/game/1"
                    :headers headers
                    :body move)]
     (is (= 400 (:status response)))
     (is (= "Invalid player" (:body response))))))

(deftest post-valid-move-with-valid-cookie
  (with-redefs [interceptors/database mocked-db]
   (swap! mocked-db assoc-in ["1" :turn] :red)
   (let [move "{:turn :red :x 0 :y 0}"
         req-headers {"Accept" "application/edn"
                      "Content-Type" "application/edn"
                      "Cookie" "player_1=red"}
         response (response-for service
                    :post "/game/1"
                    :headers req-headers
                    :body move)
         raw-cookie (first (get-in response [:headers "Set-Cookie"]))]
     (is (= 200 (:status response)))
     (is (not (empty? raw-cookie))))))


(deftest post-invalid-move-with-valid-cookie
  (with-redefs [interceptors/database mocked-db]
   (swap! mocked-db assoc-in ["1" :turn] :blue)
   (let [move "{:turn :red :x 1 :y 1}"
         req-headers {"Accept" "application/edn"
                      "Content-Type" "application/edn"
                      "Cookie" "player_1=red"}
         response (response-for service
                    :post "/game/1"
                    :headers req-headers
                    :body move)]
        (is (= 400 (:status response)))
        (is (= "Invalid move" (:body response))))))
