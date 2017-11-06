(ns zots.board (:require [proto-repl.saved-values]))

(defn visited?
 [x y v]
 (true? (some #(and (= (:x %) x) (= (:y %) y)) v)))

(defn add-visited
 [x y v]
 (if (visited? x y v) v (conj v {:x x :y y})))

(defn should-visit?
 "Checks if the coordinates are not outside our world"
 [x y board]
 (not (or
        (< x 0)
        (< y 0)
        (> x (count board))
        (> y (count board)))))

(defn get-target-player [state]
 (let [[tx ty] (:target state)]
   (get-in state [:board ty tx :player])))

(defn can-fill?
 [cell target-color]
 (or
   (= (:player cell) target-color)
   (= (:player cell) :none)))

(defn flood-cell?
 [x y state]
 (let [cell (get-in state [:board y x])
       target-color (get-target-player state)]
   (and
     (can-fill? cell target-color)
     (not (visited? x y (:visited state)))
     (should-visit? x y (:board state)))))

(defn check-cell
 [[x y] state]
 (if (flood-cell? x y state)
   (fill-flood x y state)
   state))

(defn touch-border?
 [[x y] board]
 (or
  (zero? x) (zero? y)
  (= y (count board))
  (= x (count (first board)))))

(defn reach-border?
 [trail board]
 (true? (some #(touch-border? % board) trail)))

(defn update-trail
 ([x y state]
  (let [cell (get-in state [:board y x])
        [tx ty] (:target state)
        target-color (get-in state [:board ty tx :player])]
   (if (can-fill? cell target-color)
     (update-in state [:trail] conj [x y])
     state))))

(defn fill-flood
 [x y state]
 (let [new-visited (add-visited x y (:visited state))
       new-state (update-trail x y state)]
   (->> (assoc-in new-state [:visited] new-visited)
     (check-cell [x (inc y)])
     (check-cell [(inc x) y])
     (check-cell [x (dec y)])
     (check-cell [(dec x) y]))))

(defn mark-surrounded
 [x y state]
 (let [touches (map #(touch-border? % (:board state)) (:trail state))]
  (if (not-every? false? touches)
    state
    (assoc-in state [:board y x :surrounded] true))))

(defn mark-as-wall
 [cell target-player]
 (if (and
       (not= (:player cell) target-player)
       (not= (:status cell) :wall)
       (not= (:player cell) :none))
  :wall
  (:status cell)))

(defn collect-cells-around
 [x y state]
 [(get-in state [:board (inc y) x])
  (get-in state [:board (dec y) x])
  (get-in state [:board y (inc x)])
  (get-in state [:board y (dec x)])
  (get-in state [:board (inc y) (inc x)])
  (get-in state [:board (dec y) (dec x)])
  (get-in state [:board (dec y) (inc x)])
  (get-in state [:board (inc y) (dec x)])])

(defn mark-wall-around-cell
 [[x y] state]
 (let [targets (collect-cells-around x y state)
       target-player (get-target-player state)]
   (reduce
    (fn [result cell]
     (assoc-in result [:board (:y cell) (:x cell) :status] (mark-as-wall cell target-player)))
    trailed
    targets)))

(defn mark-walls-around-trail
 [state]
 (if (reach-border? (:trail state) (:board state))
   state
   (map #(mark-wall-around-cell % state) (:trail state))))

(defn parse-cell
 [x y state]
 (->> state
  (fill-flood x y)
  (mark-surrounded x y)
  (mark-walls-around-trail)))

(def test-state
 {:board simple-surround
  :target [1 1]
  :visited []
  :trail []})

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

(defn next-state
 [state]
 (loop [state state
        cells (flatten (:board state))]
  (let [target (first cells)
        x (:x target)
        y (:y target)]
    (if (empty? cells)
      state
      (recur (parse-cell x y state) (rest cells))))))
