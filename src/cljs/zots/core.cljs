(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.zots.util :refer [coord->screen get-player-cookie document-hidden?]]
            [cljs.spec.alpha :as s]
            [cljs.core.async :refer [chan close! <!]]
            [cljs.zots.reconciler :refer [reconciler send cb-merge]]
            [cljs.zots.config :as config])
  (:require-macros
            [cljs.core.async.macros :as m :refer [go]])
  (:import [goog.net XhrIo]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")
    (s/check-asserts true)))

(def zot-red-color "#FF4317")
(def zot-blue-color "#6137BC")

(defn zot-class
 [props]
 (let [status (:status props)
       player (name (:player props))
       surrounded? (:surrounded? props)]
   (cond
    (and (status #{:active :wall}) (false? surrounded?)) player
    (true? surrounded?) (str player " surrounded"))))

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
 (map (fn [[[x y] {:keys [player status surrounded?]}]]
        (zot {:react-key (str "x" x "y" y)
              :x (coord->screen x)
              :y (coord->screen y)
              :player player
              :status status
              :surrounded? surrounded?}))
      board))

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
  (let [{:keys [board walls player turn]} (om/props this)
        cl (if (= player turn) "active" "passive")
        cl (str "board " cl)]
    (dom/div #js {:className "board"}
      (dom/svg #js {:width "550px" :height "650px" :className cl}
        (zots board)
        (walls-ui walls :red)
        (walls-ui walls :blue))))))

(def board-ui (om/factory Board))

(defn turn-text
 [pl turn]
 (let [class (str (name pl) "_turn" " text")]
   (if (or (nil? pl) (= pl :none))
     {:class "none text" :text "OBSERVER"}
     (if (= pl turn)
      {:class class :text "YOUR TURN"}
      {:class class :text "NOT YOUR TURN"}))))

(defui Current-turn
  Object
  (render [this]
   (let [{:keys [turn player]} (om/props this)
         {:keys [class text]} (turn-text player turn)]
     (dom/div #js {:className class} text))))

(def current-turn (om/factory Current-turn))

(defn draw-score-cell
  ([score player] (draw-score-cell score player ""))
  ([score player line-style]
   (let [pl (name player)
         wcl (str line-style " " (wall-class pl))
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

       (dom/text #js {:x 20 :y 42 :className txtcl} score)))))

(defui BluePlayButton
  Object
  (render [this]
    (let [pl "blue"
          wcl (wall-class pl)
          color (wall-stroke-color :blue)]
      (dom/svg #js
        {
          :width "100px"
          :height "100px"
          :className "hover_group blue_play_button"
          :onClick
          (fn [e]
            (om/transact! this '[(blue-play-button/click)]))}
        (dom/circle #js {:cx 5 :cy 5 :r 4 :className pl})
        (dom/line #js {:x1 5 :y1 5 :x2 75 :y2 5 :stroke color :className wcl})
        (dom/circle #js {:cx 75 :cy 5 :r 4 :className pl})

        (dom/line #js {:x1 5 :y1 5 :x2 5 :y2 50 :stroke color :className wcl})
        (dom/line #js {:x1 75 :y1 5 :x2 75 :y2 50 :stroke color :className wcl})

        (dom/circle #js {:cx 5 :cy 50 :r 4 :className pl})
        (dom/line #js {:x1 5 :y1 50 :x2 75 :y2 50 :stroke color :className wcl})
        (dom/circle #js {:cx 75 :cy 50 :r 4 :className pl})

        (dom/text #js {:x 10 :y 37 :className "blue long-text"} "PLAY")))))

(def blue-play-button (om/factory BluePlayButton))


(defn all-joined?
  [slots]
  (= #{:blue :red :none} slots))

(defui BlueSlot
  Object
  (render [this]
    (let [{:keys [score slots player]} (om/props this)]
      (cond
        (or (all-joined? slots) (= :blue player))
        (draw-score-cell score :blue)
        (= :none player) (blue-play-button)
        :else (draw-score-cell score :blue "empty_slot")))))

(def blue-slot (om/factory BlueSlot))

(defui ScoreBoard
 Object
 (render [this]
   (let [{:keys [score slots player]} (om/props this)]
    (dom/div #js {:className "score-board"}
      (draw-score-cell (:red score) :red)
      (blue-slot {:score (:blue score) :slots slots :player player})))))

(def score-board (om/factory ScoreBoard))

(defui NotificaitonBox
  Object
  (render [this]
    (dom/div #js {:className "notification-box text"}
      "To start the game: send the link to Player 2")))

(def notification-box (om/factory NotificaitonBox))

(defn show-notification?
  [slots player]
  (and (not (all-joined? slots)) (= :red player)))

(defui Header
 Object
 (render [this]
  (let [{:keys [turn score slots player]} (om/props this)]
    (dom/div nil
      (when (show-notification? slots player) (notification-box))
      (score-board {:score score :slots slots :player player :react-key "score-board"})
      (current-turn {:turn turn :player player})))))

(def header (om/factory Header))

(defui Game
 static om/IQuery
 (query [this]
  [:board :walls :score :turn :slots])
 Object
 (render [this]
  (let [{:keys [board walls score turn slots]} (om/props this)
        pl (get-player-cookie)]
    (dom/div nil
     (header {:score score :turn turn :slots slots :player pl})
     (board-ui {:board board
                :walls walls
                :slots slots
                :player pl
                :turn turn})))))

(def game (om/factory Game))

(om/add-root! reconciler Game (gdom/getElement "app"))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(defn determine-delay
  "Since I'm currently doing polling I don't want to hit the server
  too often is a player is in another tab. But if player is actively playing
  we make the delay shorter."
  []
  (if (document-hidden?) 60000 15000))

;;;;; Endless loop
(go
 (loop []
   (<! (timeout (determine-delay)))
   (send {:get (om/get-query Game)} cb-merge)
   (recur)))
