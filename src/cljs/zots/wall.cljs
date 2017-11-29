(ns cljs.zots.wall)

(defn same-cell-coord?
 [c1 c2]
 (and (= (:x c1) (:x c2))
      (= (:y c1) (:y c2))))

(defn in-list?
 [lst node]
 (not (empty? (filter #(same-cell-coord? node %) lst))))

(defn in-graph?
 [g x]
 (not (empty?
        (filter
          #(in-list? (get g %) x)
          (keys g)))))

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
 (reduce
   (fn [res c] (add-vertex res c (find-connections walls c))) {} walls))

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
 (flatten (map #(build-wall walls %) (keys walls))))

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
 (as->
   (filter #(missing-src? walls (:dst %)) walls) m
   (conj walls
         (assoc (first m) :src (:dst (second m)))
         (assoc (second m) :src (:dst (first m))))))

(defn get-walls
 [board pl]
 (-> (get-walls-graph board pl)
     (build-walls)
     (add-missing-walls)))
