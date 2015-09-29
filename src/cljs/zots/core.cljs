(ns ^:figwheel-always zots.core
    (:require [cljs.reader :as reader]
              [goog.events :as events]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true])
    (:import [goog.net XhrIo]
             goog.net.EventType
             [goog.events EventType]))

(enable-console-print!)

(defonce app-state 
  (atom 
    {:cells
      [{:pos [0 0] :user "player2"}
       {:pos [0 1] :user nil}
       {:pos [0 2] :user nil}
       {:pos [0 3] :user nil}
       {:pos [1 0] :user nil}
       {:pos [1 1] :user nil}
       {:pos [1 2] :user nil}
       {:pos [1 3] :user nil}]
     :length 4}))

(defn take-over-cell
  [data cell user]
  (if (nil? (:user cell))
    (let [pos (.indexOf (to-array data) @cell)]
      (assoc-in data [pos :user] user))
    data))

(defn hit-cell
  [cell owner]
  (let [cursor (om/ref-cursor (:cells (om/root-cursor app-state)))]
    (om/transact! cursor #(take-over-cell (:cells @app-state) cell "player1"))))

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
  (let [rows (partition (:length data) (:cells data))]
    (reify
      om/IRender
      (render [this]
        (apply dom/div nil 
          (om/build-all row-view rows))))))

(om/root board-view app-state
  {:target (. js/document (getElementById "app"))})

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)

