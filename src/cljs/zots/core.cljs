(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.zots.wall :as wall]
            [cljs.zots.util :refer [coord->screen]]
            [cljs.zots.muties :as muties]))

(enable-console-print!)

(defn empty-zot
 [x y]
 {:x x :y y :surrounded false :status :active :player :none})

(defn empty-row
 [y]
 (vec (map #(empty-zot % y) (range 0 17))))

(defn gen-empty-state
 []
 (vec (map #(empty-row %) (range 0 20))))

(def game-state
 (atom
  {:board (gen-empty-state)
   :turn :red
   :score {:red 0 :blue 0}
   :walls {:red '() :blud '()}}))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
   (if-let [[_ value] (find st key)]
     {:value value}
     {:value :not-found})))

(defui GameTitle
  Object
  (render [this]
    (dom/div nil "Battle of Zots")))

(def game-title (om/factory GameTitle))

(defui Zot
 Object
 (render [this]
  (let [{:keys [x y player] :as props} (om/props this)]
   (dom/circle
     #js {:cx x
          :cy y
          :strokeWidth 5
          :className (str (name player) " hover_group")
          :onClick (fn [e] (om/transact! this `[(zots/click ~props)]))}))))

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

(defn walls-ui
 [ws pl]
 (map #(build-wall (:src %) (:dst %) pl) (get ws pl)))

(defn zots
 [board]
 (map (fn [{:keys [x y player]}]
        (zot {:react-key (str x player y)
              :x (coord->screen x)
              :y (coord->screen y)
              :player player}))
      (flatten board)))

(defui Board
 Object
 (render [this]
  (let [{:keys [board walls]} (om/props this)]
    (dom/svg #js {:width "1000px" :height "1000px"}
      (zots board)
      (walls-ui walls :red)
      (walls-ui walls :blue)))))

(def board-ui (om/factory Board))

(defui Game
 static om/IQuery
 (query [this]
  [:board :walls])
 Object
 (render [this]
  (let [{:keys [board walls]} (om/props this)]
    (dom/div nil
     (board-ui {:react-key "game"
                :board board
                :walls walls})))))

(def game (om/factory Game))

(def reconciler
 (om/reconciler
  {:state game-state
   :parser (om/parser {:read read :mutate muties/mutate})}))

(om/add-root! reconciler
 Game (gdom/getElement "app"))
