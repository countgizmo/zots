(ns cljs.zots.muties
  (:require [om.next :as om]
            [cljs.zots.util :refer [screen->coord]]
            [cljc.zots.board :as board]))

(defn toggle-turn [pl] (if (= pl :red) :blue :red))

(defn can-take?
 [zot me]
 (and
  (= (:player zot) :none)
  (= (:status zot) :active)))

(defn make-state
 ([b] (make-state b nil))
 ([b t]
  {:board b
   :target t
   :visited []}))

(defmulti mutate om/dispatch)

(defmethod mutate 'zots/click
  [{:keys [state]} _ {:keys [x y]}]
  (let [x (screen->coord x)
        y (screen->coord y)
        me (:turn @state)
        zot (get-in @state [:board y x])]
    {:action
     (fn []
      (if (can-take? zot me)
        (swap! state assoc-in
         [:board y x :player] me))
      (swap! state assoc :board (-> (board/next-state @state) :board)))}))

(defmethod mutate 'test-switch/click
  [{:keys [state]} _ {:keys [turn]}]
  {:action
   (fn []
    (swap! state assoc :turn turn))})
