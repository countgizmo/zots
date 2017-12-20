(ns cljc.zots.wall
  (:require [clojure.spec.alpha :as s]
            [cljc.zots.specs :as specs]))

(def test-walls
 [[{:x 0, :y 0, :surrounded false, :status :active, :player :none}
   {:x 1, :y 0, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 0, :surrounded false, :status :active, :player :none}
   {:x 3, :y 0, :surrounded false, :status :active, :player :none}
   {:x 4, :y 0, :surrounded false, :status :wall, :player :red}
   {:x 5, :y 0, :surrounded false, :status :active, :player :none}]
  [{:x 0, :y 1, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 1, :surrounded true, :status :active, :player :blue}
   {:x 2, :y 1, :surrounded false, :status :wall, :player :red}
   {:x 3, :y 1, :surrounded false, :status :wall, :player :red}
   {:x 4, :y 1, :surrounded true, :status :active, :player :blue}
   {:x 5, :y 1, :surrounded false, :status :wall, :player :red}]
  [{:x 0, :y 2, :surrounded false, :status :active, :player :none}
   {:x 1, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 2, :surrounded false, :status :active, :player :none}
   {:x 3, :y 2, :surrounded false, :status :active, :player :none}
   {:x 4, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 5, :y 2, :surrounded false, :status :active, :player :none}]])


(defn same-cell-coord?
 [c1 c2]
 {:pre [(s/assert ::specs/cell c1) (s/assert ::specs/cell c2)]}
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

(defn walls-of
 [board player]
 (filter
   #(and (= player (:player %)) (= :wall (:status %)))
   (flatten board)))

(defn left-most-ind
 [fb]
 (->> (map-indexed (fn [ind val] [ind (:x val) (:y val)]) fb)
      (reduce
       (fn [[i x y] [i1 x1 y1]]
         (cond
          (= x x1) (if (< y y1) [i x y] [i1 x1 y1])
          (< x x1) [i x y]
          :else [i1 x1 y1])))
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


(defn dist-sq
  "Return square of distance"
  [p1 p2]
  (+
   (* (- (:x p1) (:x p2)) (- (:x p1) (:x p2)))
   (* (- (:y p1) (:y p2)) (- (:y p1) (:y p2)))))

(defn compare-dist
 [p p1 p2]
 (if (>= (dist-sq p p2) (dist-sq p p1)) -1 1))

(defn compare-points
 [p p1 p2]
 (let [orientation (orientation p p1 p2)]
   (cond
    (= orientation 2) -1
    (= orientation 0) (compare-dist p p1 p2)
    :else 1)))

(defn get-closest
 [coll {:keys [x y]}]
 (filter
  #(and
     (<= (:x %) (inc x))
     (<= (:y %) (inc y)))
   coll))

(defn valid-candidates-idx
 [fb ind1 ind2]
 (let [c1 (nth fb ind1)
       c2 (nth fb ind2)]
   (keep-indexed #(if (or (nearest-wall? c2 %2) (nearest-wall? c1 %2)) %1) fb)))

(defn find-next-p
 [coll p]
 (let [np (next-ind coll p)
       inds (range 0 (count coll))]
   (reduce
    (fn [res cell]
     (if
       (and
         (nearest-wall? (coll p) (coll cell))
         (counter-clockwise? (coll p) (coll cell) (coll res)))
       cell res))
    np
    inds)))

(defn new-orientation
 "Orientation of p in regard to line ab."
 [a b p]
 (let [x1 (:x a) y1 (:y a)
       x2 (:x b) y2 (:y b)
       x (:x p) y (:y p)]
   (-
     (* (- x x1) (- y2 y1))
     (* (- y y1) (- x2 x1)))))

(defn on-left-side? [a b p] (> 0 (new-orientation a b p)))

(defn all-on-right-side?
 [a b coll]
 (let [f (partial new-orientation a b)]
   (every? #(<= 0 (f %)) coll)))

(defn all-on-left-side?
 [a b coll]
 (let [f (partial new-orientation a b)]
   (every? #(>= 0 (f %)) coll)))

(defn active-range
 [v p]
 (if (= p (dec (count v)))
   (range 0 p)
   (range p (count v))))

(defn candidates
 [v p]
 (filter #(nearest-wall? (v p) (v %)) (active-range v p)))

(defn scan-zone
 [v p]
 (let [r (active-range v p)]
  (subvec v (first r) (inc (last r)))))

(defn find-next
 [v p f]
 (reduce
  #(if (f (v p) (v %2) (scan-zone v p)) %2 %1)
  (candidates v p)))

(defn coord [{:keys [x y]}] [x y])

(defn outline
 [v start f]
 (reduce
  (fn [res ind]
    (if (= (last res) (dec (count v)))
      res
      (conj res (find-next v ind f))))
  [start]
  (range start (- (count v) 2))))

(defn outline-recur
 [v start f]
 (loop [res [start] p start]
  (let [np (find-next v p f)]
    (if (= np (dec (count v)))
     (conj res np)
     (recur (conj res np) np)))))

; (range 0 (- (count c) 2))
; (find-next c 1 all-on-right-side?)
; (outline-recur c 0 all-on-right-side?)
; (outline-recur c 0 all-on-left-side?)
;
; (outline-recur (first cs) 0 all-on-left-side?)
; (outline-recur (first cs) 0 all-on-right-side?)
;
; (outline c 0 all-on-right-side?)
; (outline c 0 all-on-left-side?)
;
; (some #(new-orientation (c 1) (c 2) %) c)
;
; (on-right-side? (c 1) (c 3) (c 5))
; (on-left-side? (c 0) (c 2) (c 4))
; (all-on-right-side? (c 1) (c 3) c)
; (candidates c 0)
; (scan-zone c 0)

(defn outline->walls
 [v idx]
 (reduce
  (fn [res [a b]]
   (conj res
    {:src (coord (get v a))
     :dst (coord (get v b))}))
  []
  (partition 2 1 idx)))

(defn walls-around
 ([v] (walls-around v (left-most-ind v)))
 ([v start]
  (concat
    (->>
      (outline-recur v start all-on-right-side?)
      (outline->walls v))
    (->>
     (outline-recur v start all-on-left-side?)
     (outline->walls v)))))

(defn convex-hull
 [coll]
 (let [start (left-most-ind coll)]
   (loop [pi start qi (find-next-p coll start) cx-hull (conj [] (coll start))]
     (if (= qi start)
       cx-hull
       (recur qi (find-next-p coll qi) (conj cx-hull (coll qi)))))))




(defn hull->walls
  [hull]
  (reduce
   (fn [res ind]
    (conj res
     {:src (coord (get hull ind))
      :dst (coord (get hull (inc ind) (first hull)))}))
   []
   (range 0 (count hull))))



(defn close-to-any?
 [coll x]
 (not
  (empty?
    (filter #(or (same-cell-coord? x %) (nearest-wall? x %)) coll))))

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
 (println "walls to clusters")
 (reduce insert-wall-in-clusters [] walls))

(defn sort-walls
 [walls]
 (sort-by (juxt :x :y) walls))

(defn get-walls
 [board player]
 (let [walls (walls-of board player)]
   (when-not (empty? walls)
     (->> walls
          (sort-walls)
          (walls->clusters)
          (map walls-around)))))

(def cs (walls->clusters (sort-walls (walls-of test-walls :red))))
(def c (first cs))

(def test-data
  [[{:x 0, :y 0, :surrounded false, :status :active, :player :none}
    {:x 1, :y 0, :surrounded false, :status :wall, :player :red}
    {:x 2, :y 0, :surrounded false, :status :active, :player :none}
    {:x 3, :y 0, :surrounded false, :status :active, :player :none}
    {:x 4, :y 0, :surrounded false, :status :wall, :player :red}
    {:x 5, :y 0, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 1, :surrounded false, :status :wall, :player :red}
    {:x 1, :y 1, :surrounded true, :status :active, :player :blue}
    {:x 2, :y 1, :surrounded false, :status :wall, :player :red}
    {:x 3, :y 1, :surrounded false, :status :wall, :player :red}
    {:x 4, :y 1, :surrounded true, :status :active, :player :blue}
    {:x 5, :y 1, :surrounded false, :status :wall, :player :red}]
   [{:x 0, :y 2, :surrounded false, :status :active, :player :none}
    {:x 1, :y 2, :surrounded false, :status :wall, :player :red}
    {:x 2, :y 2, :surrounded false, :status :active, :player :none}
    {:x 3, :y 2, :surrounded false, :status :active, :player :none}
    {:x 4, :y 2, :surrounded false, :status :wall, :player :red}
    {:x 5, :y 2, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 3, :surrounded false, :status :active, :player :none}
    {:x 1, :y 3, :surrounded false, :status :active, :player :none}
    {:x 2, :y 3, :surrounded false, :status :active, :player :none}
    {:x 3, :y 3, :surrounded false, :status :active, :player :none}
    {:x 4, :y 3, :surrounded false, :status :active, :player :none}
    {:x 5, :y 3, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 4, :surrounded false, :status :active, :player :none}
    {:x 1, :y 4, :surrounded false, :status :active, :player :none}
    {:x 2, :y 4, :surrounded false, :status :wall, :player :red}
    {:x 3, :y 4, :surrounded false, :status :wall, :player :red}
    {:x 4, :y 4, :surrounded false, :status :wall, :player :red}
    {:x 5, :y 4, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 5, :surrounded false, :status :active, :player :none}
    {:x 1, :y 5, :surrounded false, :status :active, :player :none}
    {:x 2, :y 5, :surrounded false, :status :wall, :player :red}
    {:x 3, :y 5, :surrounded true, :status :active, :player :blue}
    {:x 4, :y 5, :surrounded false, :status :wall, :player :red}
    {:x 5, :y 5, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 6, :surrounded false, :status :active, :player :none}
    {:x 1, :y 6, :surrounded false, :status :active, :player :none}
    {:x 2, :y 6, :surrounded false, :status :active, :player :none}
    {:x 3, :y 6, :surrounded false, :status :wall, :player :red}
    {:x 4, :y 6, :surrounded false, :status :active, :player :none}
    {:x 5, :y 6, :surrounded false, :status :active, :player :none}]])
