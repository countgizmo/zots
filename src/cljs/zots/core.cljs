(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.zots.wall :as wall]))

(def not-so-simple-surround
 [[{:y 0, :surrounded false, :status :active, :player :red, :x 0}
   {:y 0, :surrounded false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded false, :status :active, :player :none, :x 2}
   {:y 0, :surrounded false, :status :active, :player :none, :x 3}]
  [{:y 1, :surrounded false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded false, :status :active, :player :blue, :x 1}
   {:y 1, :surrounded false, :status :active, :player :red, :x 2}
   {:y 1, :surrounded false, :status :active, :player :none, :x 3}]
  [{:y 2, :surrounded false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded false, :status :active, :player :none, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 2}
   {:y 2, :surrounded false, :status :active, :player :none, :x 3}]
  [{:y 3, :surrounded false, :status :active, :player :none, :x 0}
   {:y 3, :surrounded false, :status :active, :player :red, :x 1}
   {:y 3, :surrounded false, :status :active, :player :red, :x 2}
   {:y 3, :surrounded false, :status :active, :player :none, :x 3}]])

(def already-surrounded
 (-> not-so-simple-surround
     (assoc-in [1 1 :surrounded] true)
     (assoc-in [0 1 :status] :wall)
     (assoc-in [0 0 :status] :wall)
     (assoc-in [1 0 :status] :wall)
     (assoc-in [1 2 :status] :wall)
     (assoc-in [2 0 :status] :wall)
     (assoc-in [2 1 :surrounded] true)
     (assoc-in [2 2 :status] :wall)
     (assoc-in [3 1 :status] :wall)
     (assoc-in [3 2 :status] :wall)))

(def game-state
 {:board already-surrounded
  :turn :red
  :score {:red 12 :blue 5}
  :walls {:red (wall/get-walls {:board already-surrounded} :red)}})

(defn coord->screen [n] (* 30 (inc n)))

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

(defn build-wall
 [[x1 y1] [x2 y2] pl]
 (wall (hash-map
         :x1 (coord->screen x1)
         :y1 (coord->screen y1)
         :x2 (coord->screen x2)
         :y2 (coord->screen y2)
         :player pl
         :react-key (str (name pl) "wall-" x1 y1 x2 y2))))

(defn walls
 [ws pl]
 (map #(build-wall (:src %) (:dst %) pl) (get ws pl)))

(defn zots
 [board]
 (map (fn [{:keys [x y player]}]
        (zot {:react-key (str x y)
              :x (coord->screen x)
              :y (coord->screen y)
              :player player}))
      (flatten board)))

(defui Board
 Object
 (render [this]
  (dom/svg nil
    (zots (get (om/props this) :board))
    (walls (get (om/props this) :walls) :red))))


(def board (om/factory Board))

(defui Game
 Object
 (render [this]
  (dom/div nil
   (game-title)
   (board {:react-key "game"
           :board (:board game-state)
           :walls (:walls game-state)}))))

(def game (om/factory Game))

(js/ReactDOM.render (game) (gdom/getElement "app"))
