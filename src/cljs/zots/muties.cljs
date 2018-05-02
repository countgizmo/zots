(ns cljs.zots.muties
  (:require [om.next :as om]
            [cljs.zots.util :refer [screen->coord]]
            [cljc.zots.board :as board]
            [cljc.zots.game :as game]
            [cljs.zots.util :refer [get-player-cookie set-player-cookie]]))

(defmulti mutate om/dispatch)

(defmethod mutate 'zots/click
  [{:keys [state ast]} _ {:keys [x y]}]
  (let [x (screen->coord x)
        y (screen->coord y)
        pl (get-player-cookie)
        move {:x x :y y :turn pl}
        st @state]
    {:post (assoc ast :params move)
     :action
     (fn []
      (when (game/valid-move? st move)
        (reset! state (game/make-move st move))))}))

(defmethod mutate 'blue-play-button/click
  [{:keys [state]} _ _]
  {:action
    (fn []
      (set-player-cookie :blue)
      (swap! state update :slots conj :blue))})
