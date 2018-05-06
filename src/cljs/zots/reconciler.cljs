(ns cljs.zots.reconciler
  (:require [om.next :as om]
            [cljs.reader :refer [read-string]]
            [cljs.zots.muties :as muties]
            [cljs.zots.util :refer [get-url
                                    set-tab-notification
                                    clear-tab-notification]])
  (:import [goog.net XhrIo]))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
   (if-let [[_ value] (find st key)]
     {:value value}
     {:get true})))

(defn query->move
  [q]
  (-> (om/query->ast q)
      (get-in [:children 0 :params])))

(defn send-request
  [payload method query cb]
  (let [headers {"Accept" "application/edn"
                 "Content-Type" "application/edn"}
        payload (query->move payload)
        xhr-cb (fn [_]
                 (this-as this
                  (let [res (read-string (.getResponseText this))]
                    (cb res query))))]
    (.send XhrIo (get-url) xhr-cb method payload headers)))

(defn send
  [query cb]
  (cond
   (:get query)
   (send-request (:get query) "GET" query cb)
   (:post query)
   (do
     (clear-tab-notification)
     (send-request (:post query) "POST" query cb))))

(def reconciler
  (om/reconciler
   {:state {}
    :parser (om/parser {:read read :mutate muties/mutate})
    :send send
    :remotes [:get :post]}))

(defn cb-merge
 [data query]
 (set-tab-notification (:turn data))
 (om/merge! reconciler data query))
