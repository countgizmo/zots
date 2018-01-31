(ns cljs.zots.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.reader :refer [read-string]]
            [cljc.zots.wall :as wall]
            [cljs.zots.util :refer [coord->screen get-url]]
            [cljs.zots.muties :as muties]
            [cljs.spec.alpha :as s]
            [cljs.core.async :refer [chan close! <!]])
  (:require-macros
            [cljs.core.async.macros :as m :refer [go]])
  (:import [goog.net XhrIo]))

(enable-console-print!)
(s/check-asserts true)


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
          :r 2
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
 (for [wc (get ws pl)]
   (map (fn [{:keys [:src :dst]}] (build-wall src dst pl)) wc)))

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
     (game-title)
     (header {:score score :turn turn})
     (board-ui {:board board
                :walls walls})))))

(def game (om/factory Game))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
   (if-let [[_ value] (find st key)]
     {:value value}
     {:get true})))


(def ast '{:type :root, :children [{:dispatch-key zots/click, :key zots/click, :params {:x 9, :y 2, :turn :blue}, :type :call}]})

(defn query->move
 [q]
 (-> (om/query->ast q)
     (get-in [:children 0 :params])))

(defn send-request
 [payload method query cb]
 (let [headers {"Accept" "application/edn"
                "Content-Type" "application/edn"}
       payload (query->move payload)
       xhr-cb (fn [_]
                (this-as this
                 (let [res (read-string (.getResponseText this))]
                   (cb res query))))]
   (.send XhrIo (get-url) xhr-cb method payload headers)))

(defn send
 [query cb]
 (cond
  (:get query)
  (send-request (:get query) "GET" query cb)
  (:post query)
  (send-request (:post query) "POST" query cb)))

(def reconciler
 (om/reconciler
  {:state {}
   :parser (om/parser {:read read :mutate muties/mutate})
   :send send
   :remotes [:get :post]}))

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
   (<! (timeout 5000))
   (send {:get (om/get-query Game)} cb-merge)
   (recur)))
