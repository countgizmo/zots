(ns cljc.zots.board)

(def board-width 17)
(def board-height 20)

(def max-x
  (memoize
    (fn [board] (ffirst (apply max-key ffirst board)))))

(def max-y
  (memoize
    (fn [board]
      (-> (apply max-key #(-> % first second) board)
          first
          second))))

(defn should-visit?
 "Checks if the coordinates are not outside our world"
 [x y board]
 (not (or
        (< x 0)
        (< y 0)
        (> x (max-x board))
        (> y (max-y board)))))

(defn get-target-player
 "Helper method to get the current target cell's color/player"
 [state]
 (let [[tx ty] (:target state)]
   (get-in state [:board [tx ty] :player])))

(defn can-fill?
 "You can fill cells of your own color or empty ones"
 [cell target-color]
 (or
   (= (:player cell) target-color)
   (= (:player cell) :none)
   (and
     (not= (:player cell) target-color)
     (true? (:surrounded? cell)))))

(defn flood-cell?
 "You can only flood cells that are:
    1. Not filled yet or filled by your color (already flooded).
    2. Not visited yet.
    3. Not outside the board (no overflows).
    4. Filled by surrounded enemy."
 [x y {:keys [visited board] :as state}]
 (let [cell (get board [x y])
       target-color (get-target-player state)]
   (and
     (can-fill? cell target-color)
     (nil? (get visited [x y]))
     (should-visit? x y board))))

(defn touch-border?
 "Cell touches the border when one of its coordinates is equal
  to one of border' coordinates"
 [[x y] board]
 (or
  (zero? x) (zero? y)
  (= y (max-y board))
  (= x (max-x board))))

(defn reach-border?
 "Check that trail of flooded cells has reached a border."
 [trail board]
 (true? (some #(touch-border? % board) trail)))

(defn update-trail
 "Only add cell to the trail if it can be filled with your color."
 [x y {:keys [board target] :as state}]
 (let [cell (get board [x y])
       [tx ty] target
       target-color (get-in board [[tx ty] :player])]
  (if (can-fill? cell target-color)
    (update state :trail conj [x y])
    state)))

(defn flood
  [[x y] state]
  (-> (update-trail x y state)
      (update :visited conj [x y])))

(defn find-flood-targets
  [[x y] state]
  (let [next-x (inc x) prev-x (dec x)
        next-y (inc y) prev-y (dec y)]
    (cond-> []
     (flood-cell? next-x y state) (conj [next-x y])
     (flood-cell? prev-x y state) (conj [prev-x y])
     (flood-cell? x next-y state) (conj [x next-y])
     (flood-cell? x prev-y state) (conj [x prev-y]))))

(defn conj-targets
  [to-coll from-coll visited]
  (remove visited (into to-coll from-coll)))

(defn fill-flood-loop
  [x y state]
  (loop [st state targets [[x y]] visited #{}]
    (if (or (reach-border? (:trail st) (:board st))
            (empty? targets))
      st
      (let [target (first targets)
            next-targets (conj-targets
                            (rest targets)
                            (find-flood-targets target st)
                            visited)]
        (recur
          (flood target st)
          next-targets
          (conj visited target))))))

(defn surrounded?
 [state]
 (let [touches (map #(touch-border? % (:board state)) (:trail state))]
   (and (not (empty? touches)) (every? false? touches))))

(defn mark-surrounded
 "A cell is surrounded only if it cannoot touch at least one border."
 [[x y] state]
 (if (surrounded? state)
   (assoc-in state [:board [x y] :surrounded?] true)
   state))

(defn mark-as-wall
 "Only enemy cell can be marked as wall.
  Cells that are already surrounded cannot be walls."
 [target-player cell]
 (if (and
       (not= (:player cell) target-player)
       (not= (:status cell) :wall)
       (not= (:player cell) :none)
       (false? (:surrounded? cell)))
  :wall
  (:status cell)))

(defn collect-cells-around
 "Grab all cells around target coord if cells exist."
 [[x y] {:keys [board]}]
 (remove
   (fn [[x y]]
     (or
       (> x (max-x board))
       (> y (max-y board))))
   [[x (dec y)]
    [(dec x) y]
    [(dec x) (dec y)]
    [x (inc y)]
    [(inc x) y]
    [(inc x) (inc y)]
    [(inc x) (dec y)]
    [(dec x) (inc y)]]))

(defn mark-wall-around-cell
 "All cells around target that can be a wall should be marked accordingly.
  Target must be surrounded."
 [[x y] state]
 (let [targets (collect-cells-around [x y] state)
       target-player (get-target-player state)]
   (if (true? (get-in state [:board [x y] :surrounded?]))
     (reduce
      (fn [result coord]
        (->> (get-in state [:board coord])
            (mark-as-wall target-player)
            (assoc-in result [:board coord :status])))
      state
      targets)
     state)))

(defn mark-walls-around-trail
 "Every surrounded cell on a trail might have walls around it."
 [{:keys [trail board] :as state}]
 (if (or (nil? trail) (reach-border? trail board))
   state
   (loop [state state
          targets trail]
    (if (empty? targets)
      state
      (let [target (first targets)
            surrounded (mark-surrounded target state)]
        (recur (mark-wall-around-cell target surrounded) (rest targets)))))))

(defn parse-cell
  [state [x y]]
  (as-> (assoc state :target [x y]) s
    (assoc s :trail #{})
    (assoc s :visited #{})
    (fill-flood-loop x y s)
    (mark-walls-around-trail s)))

(defn taken-active?
 [[_ v]]
 (and (not= :none (:player v)) (false? (:surrounded? v))))

(defn get-cell
 [board x y]
 (get-in board [y x]))

(defn clean-state
 [{:keys [board walls turn score slots]}]
 {:board board :walls walls :turn turn :score score :slots slots})

(defn next-state
  [state]
  (loop [state state
         cells (map first (filter taken-active? (:board state)))]
    (if (empty? cells)
      (clean-state state)
      (recur (parse-cell state (first cells)) (rest cells)))))

(defn empty-zot
 [x y]
 {[x y] {:surrounded? false :player :none :status :active}})

(defn gen-empty-board
  []
  (->> (for [x (range 0 board-width)
             y (range 0 board-height)]
         (empty-zot x y))
       (into {})))

(defn board->coll-of-coord
  [board]
  (keys board))
