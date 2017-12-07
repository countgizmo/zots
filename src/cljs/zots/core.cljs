(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljc.zots.wall :as wall]
            [cljs.zots.util :refer [coord->screen]]
            [cljs.zots.muties :as muties]))

(enable-console-print!)

(defn empty-zot
 [x y]
 {:x x :y y :surrounded false :status :active :player :none})

(defn empty-row
 [y]
 (vec (map #(empty-zot % y) (range 0 17))))

(defn gen-empty-board
 []
 (vec (map #(empty-row %) (range 0 20))))

(def game-state
 (atom
  {:board (gen-empty-board)
   :turn :red
   :score {:red 0 :blue 0}
   :walls {:red '() :blue '()}}))

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


(defn zot-class
 [props]
 (let [status (:status props)
       player (name (:player props))
       surrounded (:surrounded props)]
   (cond
    (and (= :active status) (false? surrounded)) player
    (= :wall status) (str player "_wall")
    (true? surrounded) (str player "_surrounded"))))

(defui Zot
 Object
 (render [this]
  (let [{:keys [x y] :as props} (om/props this)]
   (dom/circle
     #js {:cx x
          :cy y
          :strokeWidth 5
          :className (str (zot-class props) " hover_group")
          :onClick (fn [e] (om/transact! this `[(zots/click ~props)]))}))))

(def zot (om/factory Zot))

(defn zots
 [board]
 (map (fn [{:keys [x y player status surrounded]}]
        (zot {:react-key (str "x" x "y" y)
              :x (coord->screen x)
              :y (coord->screen y)
              :player player
              :status status
              :surrounded surrounded}))
      (flatten board)))


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

(defui Board
 Object
 (render [this]
  (let [{:keys [board walls]} (om/props this)]
    (dom/svg #js {:width "1000px" :height "1000px"}
      (zots board)
      (walls-ui walls :red)
      (walls-ui walls :blue)))))

(def board-ui (om/factory Board))

(defui Current-turn
 Object
 (render [this]
  (let [{:keys [turn]} (om/props this)]
    (dom/div nil (str "Turn: " (name turn))))))

(def current-turn (om/factory Current-turn))

(defui Turn-Switch
 Object
 (render [this]
  (let [{:keys [turn] :as props} (om/props this)]
    (dom/div nil
      (dom/label nil "Red"
        (dom/input
         #js {:checked (= :red turn)
              :type "radio"
              :name "turn-switch"
              :value :red
              :onClick
              (fn [e]
               (om/transact! this `[(test-switch/click {:turn :red})]))}))
      (dom/label nil "Blue"
        (dom/input
         #js {:checked (= :blue turn)
              :type "radio"
              :name "turn-switch"
              :value :blue
              :onClick
               (fn [e]
                (om/transact! this `[(test-switch/click {:turn :blue})]))}))))))

(def turn-switch (om/factory Turn-Switch))

(defui ScoreBoard
 Object
 (render [this]
  (let [{:keys [score]} (om/props this)]
    (dom/div #js {:className "score-board"}
      (dom/div nil (str "Blue: " (:blue score)))
      (dom/div nil (str "Red: " (:red score)))))))

(def score-board (om/factory ScoreBoard))

(defui Header
 Object
 (render [this]
  (let [{:keys [turn score]} (om/props this)]
    (dom/div nil
     (score-board {:score score :react-key "score-board"})
     (current-turn {:turn turn})
     (turn-switch {:turn turn})))))

(def header (om/factory Header))

(defui Game
 static om/IQuery
 (query [this]
  [:board :walls :score :turn])
 Object
 (render [this]
  (let [{:keys [board walls score turn]} (om/props this)]
    (dom/div nil
     (game-title)
     (header {:score score :turn turn})
     (board-ui {:react-key "game"
                :board board
                :walls {:red (wall/get-walls board :red)
                        :blue (wall/get-walls board :blue)}})))))

(def game (om/factory Game))

(def reconciler
 (om/reconciler
  {:state game-state
   :parser (om/parser {:read read :mutate muties/mutate})}))

(om/add-root! reconciler
 Game (gdom/getElement "app"))
