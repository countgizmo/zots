(ns cljs.zots.muties
  (:require [om.next :as om]
            [cljs.zots.util :refer [screen->coord]]
            [cljc.zots.board :as board]
            [cljc.zots.game :as game]))

(defn toggle-turn [pl] (if (= pl :red) :blue :red))

(defn next-board
 [b]
 (-> (time (board/next-state {:board b}))
     :board))

(defmulti mutate om/dispatch)

(defmethod mutate 'zots/click
  [{:keys [state]} _ {:keys [x y]}]
  (let [x (screen->coord x)
        y (screen->coord y)
        me (:turn @state)
        zot (get-in @state [:board y x])]
    {:action
     (fn []
      (if (game/cell-available? zot)
        (swap! state assoc-in
         [:board y x :player] me))
      (swap! state update-in [:board] next-board))}))

(defmethod mutate 'test-switch/click
  [{:keys [state]} _ {:keys [turn]}]
  {:action
   (fn []
    (swap! state assoc :turn turn))})
