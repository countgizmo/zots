(ns cljs.zots.muties
  (:require [om.next :as om]
            [cljs.zots.util :refer [screen->coord]]
            [cljc.zots.board :as board]
            [cljc.zots.game :as game]
            [cljs.zots.util :refer [get-player-cookie]]
            [goog.net.cookies :as cks]))

(defn toggle-turn [pl] (if (= pl :red) :blue :red))

(defn next-board
 [b]
 (-> (time (board/next-state {:board b}))
     :board))

(defmulti mutate om/dispatch)

(defmethod mutate 'zots/click
  [{:keys [state ast]} _ {:keys [x y]}]
  (let [st @state
        x (screen->coord x)
        y (screen->coord y)
        pl (get-player-cookie)
        move {:x x :y y :turn pl}]
    {:post (assoc ast :params move)
     :action
     (fn []
      (when (game/valid-move? @state move)
        (reset! state (game/make-move @state move))))}))


(defmethod mutate 'test-switch/click
  [{:keys [state]} _ {:keys [turn]}]
  {:action
   (fn []
    (swap! state assoc :turn turn))})
