(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.zots.util :refer [coord->screen get-player-cookie]]
            [cljs.spec.alpha :as s]
            [cljs.core.async :refer [chan close! <!]]
            [cljs.zots.reconciler :refer [reconciler send]])
  (:require-macros
            [cljs.core.async.macros :as m :refer [go]])
  (:import [goog.net XhrIo]))

(enable-console-print!)
(s/check-asserts true)

(def zot-red-color "#FF4317")
(def zot-blue-color "#6137BC")

(defn zot-class
 [props]
 (let [status (:status props)
       player (name (:player props))
       surrounded (:surrounded props)]
   (cond
    (and (status #{:active :wall}) (false? surrounded)) player
    (true? surrounded) (str player " surrounded"))))

(defn zot-radius
 [{:keys [player]}]
 (if (= :none player) 2 5))

(defui Zot
 Object
 (render [this]
  (let [{:keys [x y] :as props} (om/props this)
        r (zot-radius props)]
   (dom/circle
     #js {:cx x
          :cy y
          :r r
          :stroke "white"
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

(defn wall-class
 [player]
 (str (name player) "_wall"))

(defn wall-stroke-color
  [player]
  (cond
   (= player :red) zot-red-color
   (= player :blue) zot-blue-color
   :else "white"))

(defui Wall
 Object
 (render [this]
  (let [{:keys [x1 x2 y1 y2 player]} (om/props this)]
    (dom/line
     #js {:x1 x1
          :y1 y1
          :x2 x2
          :y2 y2
          :stroke (wall-stroke-color player)
          :className (wall-class player)}))))

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
 (for [wc (get ws pl)]
   (map (fn [{:keys [:src :dst]}] (build-wall src dst pl)) wc)))

(defui Board
 Object
 (render [this]
  (let [{:keys [board walls]} (om/props this)]
    (dom/div #js {:className "board"}
      (dom/svg #js {:width "550px" :height "650px"}
        (zots board)
        (walls-ui walls :red)
        (walls-ui walls :blue))))))

(def board-ui (om/factory Board))

(defn turn-text
 [pl turn]
 (let [class (str (name turn) "_turn" " text")]
   (if (or (nil? pl) (= pl :none)) {:class "none text" :text "OBSERVER"}
     (if (= pl turn)
      {:class class :text "YOUR TURN"}
      {:class class :text "NOT YOUR TURN"}))))

(defui Current-turn
 Object
 (render [this]
  (let [{:keys [turn]} (om/props this)
        pl (get-player-cookie)
        {:keys [class text]} (turn-text pl turn)]
    (dom/div #js {:className class} text))))

(def current-turn (om/factory Current-turn))

(defn draw-score-cell
 [score player]
 (let [pl (name player)
       wcl (wall-class pl)
       txtcl (str pl " text")
       color (wall-stroke-color player)]
   (dom/svg #js {:width "100px" :height "100px"}
     (dom/circle #js {:cx 5 :cy 5 :r 4 :className pl})
     (dom/line #js {:x1 5 :y1 5 :x2 75 :y2 5 :stroke color :className wcl})
     (dom/circle #js {:cx 75 :cy 5 :r 4 :className pl})

     (dom/line #js {:x1 5 :y1 5 :x2 5 :y2 50 :stroke color :className wcl})
     (dom/line #js {:x1 75 :y1 5 :x2 75 :y2 50 :stroke color :className wcl})

     (dom/circle #js {:cx 5 :cy 50 :r 4 :className pl})
     (dom/line #js {:x1 5 :y1 50 :x2 75 :y2 50 :stroke color :className wcl})
     (dom/circle #js {:cx 75 :cy 50 :r 4 :className pl})

     (dom/text #js {:x 20 :y 42 :className txtcl} score))))

(defui ScoreBoard
 Object
 (render [this]
  (let [{:keys [score]} (om/props this)]
    (dom/div #js {:className "score-board"}
      (draw-score-cell (:red score) :red)
      (draw-score-cell (:blue score) :blue)))))

(def score-board (om/factory ScoreBoard))

(defui Header
 Object
 (render [this]
  (let [{:keys [turn score]} (om/props this)]
    (dom/div nil
     (score-board {:score score :react-key "score-board"})
     (current-turn {:turn turn})))))

(def header (om/factory Header))

(defui Game
 static om/IQuery
 (query [this]
  [:board :walls :score :turn])
 Object
 (render [this]
  (let [{:keys [board walls score turn]} (om/props this)]
    (dom/div nil
     (header {:score score :turn turn})
     (board-ui {:board board
                :walls walls})))))

(def game (om/factory Game))



(om/add-root! reconciler
 Game (gdom/getElement "app"))

(defn cb-merge
 [data query]
 (om/merge! reconciler data query))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

;;;;; Endless loop
(go
 (loop []
   (<! (timeout 15000))
   (send {:get (om/get-query Game)} cb-merge)
   (recur)))
