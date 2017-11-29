(ns cljc.zots.board)

(defn visited?
 [x y v]
 (true? (some #(and (= (first %) x) (= (second %) y)) v)))

(defn add-visited
 [x y v]
 (if (visited? x y v) v (conj v [x y])))

(defn should-visit?
 "Checks if the coordinates are not outside our world"
 [x y board]
 (not (or
        (< x 0)
        (< y 0)
        (> x (dec (count board)))
        (> y (dec (count board))))))

(defn get-target-player [state]
 "Helper method to get the current target cell's color/player"
 (let [[tx ty] (:target state)]
   (get-in state [:board ty tx :player])))

(defn can-fill?
 "You can fill cells of your own color or empty ones"
 [cell target-color]
 (or
   (= (:player cell) target-color)
   (= (:player cell) :none)))

(defn flood-cell?
 "You can only flood cells that are:
    1. Not filled yet or filled by your color (already flooded).
    2. Not visited yet.
    3. Not outside the board (no overflows)."
 [x y state]
 (let [cell (get-in state [:board y x])
       target-color (get-target-player state)]
   (and
     (can-fill? cell target-color)
     (not (visited? x y (:visited state)))
     (should-visit? x y (:board state)))))

(defn touch-border?
 "Cell touches the border when one of its coordinates is equal
  to one of border' coordinates"
 [[x y] board]
 (or
  (zero? x) (zero? y)
  (= y (dec (count board)))
  (= x (dec (count (first board))))))

(defn reach-border?
 "Check that trail of flooded cells has reached a border."
 [trail board]
 (true? (some #(touch-border? % board) trail)))

(defn update-trail
 "Only add cell to the trail if it can be filled with your color."
 [x y state]
 (let [cell (get-in state [:board y x])
       [tx ty] (:target state)
       target-color (get-in state [:board ty tx :player])]
  (if (can-fill? cell target-color)
    (update-in state [:trail] #(vec (conj % [x y])))
    state)))

(declare check-cell)

(defn fill-flood
 [x y state]
 (let [new-visited (add-visited x y (:visited state))
       new-state (update-trail x y state)]
   (->> (assoc-in new-state [:visited] new-visited)
     (check-cell [x (inc y)])
     (check-cell [(inc x) y])
     (check-cell [x (dec y)])
     (check-cell [(dec x) y]))))

(defn check-cell
 [[x y] state]
 (if (flood-cell? x y state)
   (fill-flood x y state)
   state))

(defn surrounded?
 [state [x y]]
 (let [touches (map #(touch-border? % (:board state)) (:trail state))]
   (and (not (empty? touches)) (every? false? touches))))

(defn mark-surrounded
 "A cell is surrounded only if it cannoot touch at least one border."
 [[x y] state]
 (let [touches (map #(touch-border? % (:board state)) (:trail state))]
  (if (surrounded? state [x y])
    (assoc-in state [:board y x :surrounded] true)
    state)))

(defn mark-as-wall
 "Only enemy cell can be marked as wall.
  Cells that are already surrounded cannot be walls."
 [cell target-player]
 (if (and
       (not= (:player cell) target-player)
       (not= (:status cell) :wall)
       (not= (:player cell) :none)
       (false? (:surrounded cell)))
  :wall
  (:status cell)))

(defn collect-cells-around
 "Grab all cells around target coord if cells exist."
 [[x y] state]
 (remove nil?
  [(get-in state [:board (inc y) x])
   (get-in state [:board (dec y) x])
   (get-in state [:board y (inc x)])
   (get-in state [:board y (dec x)])
   (get-in state [:board (inc y) (inc x)])
   (get-in state [:board (dec y) (dec x)])
   (get-in state [:board (dec y) (inc x)])
   (get-in state [:board (inc y) (dec x)])]))

(defn mark-wall-around-cell
 "All cells around target that can be a wall should be marked accordingly.
  Target must be surrounded."
 [[x y] state]
 (let [targets (collect-cells-around [x y] state)
       target-player (get-target-player state)]
   (if (true? (get-in state [:board y x :surrounded]))
     (reduce
      (fn [result cell]
       (assoc-in result [:board (:y cell) (:x cell) :status] (mark-as-wall cell target-player)))
      state
      targets)
     state)))

(defn mark-walls-around-trail
 "Every surrounded cell on a trail might have walls around it."
 [state]
 (if (reach-border? (:trail state) (:board state))
   state
   (loop [state state
          targets (:trail state)]
    (if (empty? targets)
      state
      (let [target (first targets)
            surrounded (mark-surrounded target state)]
        (recur (mark-wall-around-cell target surrounded) (rest targets)))))))

(defn parse-cell
 [state [x y]]
 (as-> (assoc state :target [x y]) s
  (assoc s :trail [])
  (assoc s :visited [])
  (fill-flood x y s)
  (mark-walls-around-trail s)))

(defn next-state
 [state]
 (loop [state state
        cells (map (fn [{:keys [x y]}] [x y]) (filter #(not= :none (:player %)) (flatten (:board state))))]
   (if (empty? cells)
     state
     (recur (parse-cell state (first cells)) (rest cells)))))
