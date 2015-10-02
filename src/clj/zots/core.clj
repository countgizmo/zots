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

(defn gen-board
  []
  (generate-response {:cells
        [{:pos [0 0] :user "player2"}
         {:pos [0 1] :user nil}
         {:pos [0 2] :user nil}
         {:pos [0 3] :user nil}
         {:pos [1 0] :user nil}
         {:pos [1 1] :user nil}
         {:pos [1 2] :user nil}
         {:pos [1 3] :user nil}]
       :length 4}))

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
