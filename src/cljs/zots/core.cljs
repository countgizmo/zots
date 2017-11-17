(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(def simple-surround
 [[{:y 0, :surrounded false, :status :active, :player :none, :x 0}
   {:y 0, :surrounded false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 1, :surrounded false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded false, :status :active, :player :blue, :x 1}
   {:y 1, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 2, :surrounded false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded false, :status :active, :player :red, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 2}]])

(def game-state
 {:board simple-surround
  :turn :red
  :score {:red 12 :blue 5}})

(defui GameTitle
  Object
  (render [this]
    (dom/div nil "Battle of Zots")))

(def game-title (om/factory GameTitle))

(defui Zot
 Object
 (render [this]
   (dom/circle
     #js {:cx (get (om/props this) :x)
          :cy (get (om/props this) :y)
          :strokeWidth 1
          :className (name (get (om/props this) :player))})))

(def zot (om/factory Zot))

(defui Wall
 Object
 (render [this]
  (dom/line
   #js {:x1 (get (om/props this) :x1)
        :y1 (get (om/props this) :y1)
        :x2 (get (om/props this) :x2)
        :y2 (get (om/props this) :y2)
        :className (name (get (om/props this) :player))})))

(def wall (om/factory Wall))

(defn zots
 [board]
 (map (fn [{:keys [x y player]}]
        (zot {:react-key (str x y)
              :x (* 30 (inc x))
              :y (* 30 (inc y))
              :player player}))
      (flatten board)))

(defui Board
 Object
 (render [this]
  (dom/svg nil
    (zots (get (om/props this) :board))
    (wall {:react-key (str "wall-" x y)
           :x1 60 :y1 30
           :x2 90 :y2 90
           :player :red}))))


(def board (om/factory Board))

(defui Game
 Object
 (render [this]
  (dom/div nil
   (game-title)
   (board {:react-key "game"
           :board (:board game-state)}))))

(def game (om/factory Game))

(js/ReactDOM.render (game) (gdom/getElement "app"))
