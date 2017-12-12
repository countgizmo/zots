(ns cljc.zots.wall)

(defn same-cell-coord?
 [c1 c2]
 (and (= (:x c1) (:x c2))
      (= (:y c1) (:y c2))))

(defn in-list?
 [lst node]
 (not (empty? (filter #(same-cell-coord? node %) lst))))

(defn nearest-wall?
 ([c1 c2] (nearest-wall? (:x c1) (:y c1) (:x c2) (:y c2)))
 ([x y x1 y1]
  (if (and (= x x1) (= y y1))
    false
    (and (>= 1 (Math/abs (- x x1)))
         (>= 1 (Math/abs (- y y1)))))))

(defn close-to-any?
 [coll x]
 (not
  (empty?
    (filter #(or (same-cell-coord? x %) (nearest-wall? x %)) coll))))

(defn walls-of
 [board player]
 (filter
   #(and (= player (:player %)) (= :wall (:status %)))
   (flatten board)))

(defn left-most-ind
 [fb]
 (->> (map-indexed (fn [ind val] [ind (:x val)]) fb)
      (reduce #(if (< (second %1) (second %2)) %1 %2))
      (first)))

(defn next-ind [fb ind] (mod (inc ind) (count fb)))

(defn orientation
 [p q r]
 (-
  (* (- (:y q) (:y p)) (- (:x r) (:x q)))
  (* (- (:x q) (:x p)) (- (:y r) (:y q)))))

(defn counter-clockwise?
 [p q r]
 (> 0 (orientation p q r)))

(defn find-next-p
 [fb p]
 (let [np (next-ind fb p)
       inds (range 0 (count fb))]
   (reduce #(if (counter-clockwise? (fb p) (fb %2) (fb %1)) %2 %1) np inds)))

(defn convex-hull
 [board]
 (let [fb (vec (flatten board))
       start (left-most-ind fb)]
   (loop [pi start qi (find-next-p fb pi) cx-hull (conj [] (fb start))]
     (if (= qi start)
       cx-hull
       (recur qi (find-next-p fb qi) (conj cx-hull (fb qi)))))))

(defn coord [{:keys [x y]}] [x y])

(defn hull->walls
  [hull]
  (reduce
   (fn [res ind]
    (conj res
     {:src (coord (get hull ind))
      :dst (coord (get hull (inc ind) (first hull)))}))
   []
   (range 0 (count hull))))

(def test-walls
  '([{:x 0, :y 0, :surrounded false, :status :wall, :player :blue}
     {:x 1, :y 0, :surrounded false, :status :wall, :player :blue}
     {:x 2, :y 0, :surrounded false, :status :active, :player :none}
     {:x 3, :y 0, :surrounded false, :status :active, :player :none}
     {:x 4, :y 0, :surrounded false, :status :active, :player :none}
     {:x 5, :y 0, :surrounded false, :status :wall, :player :blue}
     {:x 6, :y 0, :surrounded false, :status :active, :player :none}]
    [{:x 0, :y 1, :surrounded false, :status :wall, :player :blue}
     {:x 1, :y 1, :surrounded true, :status :active, :player :red}
     {:x 2, :y 1, :surrounded false, :status :wall, :player :blue}
     {:x 3, :y 1, :surrounded false, :status :active, :player :none}
     {:x 4, :y 1, :surrounded false, :status :wall, :player :blue}
     {:x 5, :y 1, :surrounded true, :status :active, :player :red}
     {:x 6, :y 1, :surrounded false, :status :wall, :player :blue}]
    [{:x 0, :y 2, :surrounded false, :status :active, :player :none}
     {:x 1, :y 2, :surrounded false, :status :wall, :player :blue}
     {:x 2, :y 2, :surrounded false, :status :wall, :player :blue}
     {:x 3, :y 2, :surrounded false, :status :active, :player :none}
     {:x 4, :y 1, :surrounded false, :status :active, :player :none}
     {:x 5, :y 2, :surrounded false, :status :wall, :player :blue}
     {:x 6, :y 2, :surrounded false, :status :active, :player :none}]))

(defn add-if-close
 [coll x]
 (if (close-to-any? coll x)
   [(conj coll x)]
   [coll]))

(defn in-clusters?
 [cs x]
 (in-list? (flatten cs) x))

(defn insert-wall-in-clusters
 [cs w]
 (if
  (close-to-any? (flatten cs) w)
  (reduce
     #(into %1 (add-if-close %2 w))
     []
     cs)
  (conj cs [w])))

(defn walls->clusters
 [walls]
 (reduce insert-wall-in-clusters [] walls))

(defn get-walls
 [board player]
 (let [walls (walls-of board player)]
   (when-not (empty? walls)
     (->> walls
          (flatten)
          (walls->clusters)
          (map #(-> (convex-hull %) (hull->walls)))))))
