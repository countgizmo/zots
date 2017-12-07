(ns cljc.zots.wall)

(defn same-cell-coord?
 [c1 c2]
 (and (= (:x c1) (:x c2))
      (= (:y c1) (:y c2))))

(defn in-list?
 [lst node]
 (not (empty? (filter #(same-cell-coord? node %) lst))))

(defn in-graph?
 [g x]
 (or (contains? g x)
   (not (empty?
          (filter
            #(in-list? (get g %) x)
            (keys g))))))

(defn de-dup-connections
 [g conns]
 (filter #(not (in-graph? g %)) conns))

(defn add-vertex
 [graph {:keys [x y]} conns]
 (let [conns (de-dup-connections graph conns)]
   (if (and (not (empty? conns)) (nil? (get graph [x y])))
     (assoc graph [x y] conns)
     graph)))

(defn nearest-wall?
 [x y x1 y1]
 (if (and (= x x1) (= y y1))
   false
   (and (>= 1 (Math/abs (- x x1)))
        (>= 1 (Math/abs (- y y1))))))

(defn diagonal?
 [x1 y1 x2 y2]
 (and (not= x1 x2) (not= y1 y2)))

(defn filter-out-diagonals
 [nodes [x y]]
 (filter
  #(diagonal? x y (:x %) (:y %))
  nodes))

(defn filter-out-extra-diagonals
 [nodes k]
 (if (< 2 (count nodes))
   (filter-out-diagonals nodes k)
   nodes))

(defn find-connections
 ([walls x y] (find-connections walls {:x x :y y}))
 ([walls {:keys [x y] :as wall}]
  (-> (filter #(nearest-wall? x y (:x %) (:y %)) walls))))

(defn build-graph
 [walls]
 (if (empty? walls) nil
   (reduce
     (fn [res c] (add-vertex res c (find-connections walls c))) {} walls)))

(defn walls-of
 [board player]
 (filter
   #(and (= player (:player %)) (= :wall (:status %)))
   (flatten board)))

(defn get-walls-graph
 [board player]
 (as-> (walls-of board player) w
       (build-graph w)))

(defn build-wall
 [walls key]
 (let [conns (get walls key)]
   (map #(hash-map :src key :dst [(:x %) (:y %)]) conns)))

(defn build-walls
 [walls]
 (if (empty? walls) '()
   (flatten (map #(build-wall walls %) (keys walls)))))

(defn missing?
 [f ws]
 (empty? (filter f ws)))

(defn missing-src?
 [walls [x y]]
 (missing? #(= (:src %) [x y]) walls))

(defn missing-dst?
 [walls [x y]]
 (missing? #(= (:dst %) [x y]) walls))

(defn add-missing-walls
 [walls]
 (if (empty? walls) (empty walls)
   (as->
     (filter #(missing-src? walls (:dst %)) walls) m
     (conj walls
           (assoc (first m) :src (:dst (second m)))
           (assoc (second m) :src (:dst (first m)))))))

(defn get-walls
 [board pl]
 (-> (get-walls-graph board pl)
     (build-walls)
     (add-missing-walls)))

(def game-state
 [[{:y 0, :surrounded false, :status :wall, :player :red, :x 0}
   {:y 0, :surrounded false, :status :wall, :player :red, :x 1}
   {:y 0, :surrounded false, :status :wall, :player :red, :x 2}]
  [{:y 1, :surrounded false, :status :wall, :player :red, :x 0}
   {:y 1, :surrounded true, :status :active, :player :blue, :x 1}
   {:y 1, :surrounded false, :status :wall, :player :red, :x 2}]
  [{:y 2, :surrounded false, :status :active, :player :none, :x 0}
   {:y 2, :surrounded false, :status :wall, :player :red, :x 1}
   {:y 2, :surrounded false, :status :wall, :player :red, :x 2}]])

(defn left-most-ind
 [board]
 (->> (flatten board)
      (map-indexed (fn [ind val] [ind (:x val)]))
      (reduce #(if (< (second %1) (second %2)) %1 %2))
      (first)))

(left-most-ind game-state)

(defn orientation
 [p q r]
 (-
  (* (- (:y q) (:y p)) (- (:x r) (:x q)))
  (* (- (:x q) (:x p)) (- (:y r) (:y q)))))

(defn counter-clockwise?
 [p q r]
 (> 0 (orientation p q r)))
