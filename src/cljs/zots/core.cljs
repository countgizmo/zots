(ns ^:figwheel-always zots.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]))

(enable-console-print!)

(defonce app-state 
  (atom 
    {:board {}}))

(defn take-over-cell
  [data cell user]
  (if (nil? (:user cell))
    (let [pos (.indexOf (to-array data) @cell)]
      (assoc-in data [pos :user] user))
    data))

(defn hit-cell
  [cell owner]
  (println @app-state)
  (let [cursor (om/ref-cursor (:cells (om/root-cursor app-state)))]
    (om/transact! cursor 
                  #(take-over-cell (:cells @app-state) cell "player1"))))

(defn cell-view
  [cell owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "column " (:user cell))
                   :onClick #(hit-cell cell owner)} nil))))

(defn row-view
  [row owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/div #js {:className "row"}
              (om/build-all cell-view row)))))

(defn board-view
  [data owner]
  (let [
        rows (partition (:length data) (:cells data))]
    (reify
      om/IRender
      (render [this]
        (apply dom/div nil 
          (om/build-all row-view rows))))))

(defn app-view [app owner]
  (reify
    om/IWillUpdate
    (will-update [_ next-props next-state]
      (when (:err-msg next-state)
        (js/setTimeout #(om/set-state! owner :err-msg nil) 5000)))
    om/IRenderState
    (render-state [_ {:keys [err-msg]}]
      (dom/div nil
        (om/build om-sync (:board app)
          {:opts {:view board-view
                  :filter (comp #{:create :update :delete} tx-tag)
                  :id-key :class/id
                  :on-success (fn [res tx-data] (println res))
                  :on-error
                  (fn [err tx-data]
                    (reset! app-state (:old-state tx-data))
                    (om/set-state! owner :err-msg
                      "Oops! Sorry, something went wrong. Try again later."))}})
         (when err-msg
           (dom/div nil err-msg))))))

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] txs))]
      (edn-xhr
        {:method :get
         :url "/init"
         :on-complete
         (fn [res]
          (reset! app-state (-> res
                                :board
                                :body
                                cljs.reader/read-string))
          (om/root board-view app-state
            {:target (.getElementById js/document "app")
             :shared {:tx-chan tx-pub-chan}
             :tx-listen
             (fn [tx-data root-cursor]
              (put! tx-chang [tx-data root-cursor]))}))}))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

