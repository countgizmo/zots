(ns cljs.zots.muties
  (:require [om.next :as om]
            [cljs.zots.util :refer [screen->coord]]))

(defn toggle-turn [pl] (if (= pl :red) :blue :red))

(defn can-take?
 [zot me]
 (and
  (= (:player zot) :none)
  (= (:status zot) :active)))

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
         [:board y x :player] me)))}))

(defmethod mutate 'test-switch/click
  [{:keys [state]} _ {:keys [turn]}]
  {:action
   (fn []
    (swap! state assoc :turn turn))})
