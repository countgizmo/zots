(ns zots.core
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [datomic.api :as d]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn gen-cell
  [col row]
  {:col col :row row :player nil :active true})

(defn gen-board
  []
  (mapv (fn [x] (mapv #(gen-cell % x) (range 0 3))) 
        (range 0 6)))

(defn init
  []
  (generate-response
    {:board (gen-board)}))

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))



(defroutes routes
  (GET "/" [] (index))
  (GET "/board" [] (gen-board))
  (GET "/init" [] (init))
  (route/files "/" {:root "resources/public"}))

(defn read-inputstream-edn [input]
  (edn/read
   {:eof nil}
   (java.io.PushbackReader.
    (java.io.InputStreamReader. input "UTF-8"))))

(defn parse-edn-body [handler]
  (fn [request]
    (handler (if-let [body (:body request)]
               (assoc request
                 :edn-body (read-inputstream-edn body))
               request))))

(def handler 
  (-> routes
      parse-edn-body))
