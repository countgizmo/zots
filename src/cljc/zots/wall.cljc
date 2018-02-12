(ns cljc.zots.wall
  (:require [clojure.spec.alpha :as s]
            [cljc.zots.specs :as specs]
            #?(:clj [proto-repl.saved-values])))

(defn same-cell-coord?
 "Checks if cells have the samme x and y coordinates."
 [c1 c2]
 {:pre [(s/assert :specs/cell c1) (s/assert :specs/cell c2)]}
 (and (= (:x c1) (:x c2))
      (= (:y c1) (:y c2))))

(defn nearest-cell?
 "Checks if two cell are near each other.
  This means no more than 1 step apart from each other in any direction."
 ([c1 c2]
  {:pre [(s/assert :specs/cell c1) (s/assert :specs/cell c2)]}
  (nearest-cell? (:x c1) (:y c1) (:x c2) (:y c2)))
 ([x y x1 y1]
  {:pre [(s/assert ::specs/x x) (s/assert ::specs/y y)]}
  (if (and (= x x1) (= y y1))
    false
    (and (>= 1 (Math/abs (- x x1)))
         (>= 1 (Math/abs (- y y1)))))))

(defn walls-of
 "Return all the walls belonging to specified player."
 [board player]
 {:pre [(s/assert ::specs/player player)]}
 (filter
   #(and (= player (:player %)) (= :wall (:status %)))
   (flatten board)))

(defn left-most-ind
 "Get index of the cell with the min x coordinate.
  In case of a tie compare y and select the one with the lowest value."
 [v]
 {:pre [(s/assert (s/coll-of :specs/cell) v)]}
 (->> (map-indexed (fn [ind val] [ind (:x val) (:y val)]) v)
      (reduce
       (fn [[i x y] [i1 x1 y1]]
         (cond
          (= x x1) (if (< y y1) [i x y] [i1 x1 y1])
          (< x x1) [i x y]
          :else [i1 x1 y1])))
      (first)))

(defn bottom-most-ind
 "Get index of the cell with the min y coordinate.
  In case of a tie compare x and select the one with the lowest value."
 [v]
 {:pre [(s/assert (s/coll-of :specs/cell) v)]}
 (->> (map-indexed (fn [ind val] [ind (:x val) (:y val)]) v)
      (reduce
       (fn [[i x y] [i1 x1 y1]]
         (cond
          (= y y1) (if (< x x1) [i x y] [i1 x1 y1])
          (< y y1) [i x y]
          :else [i1 x1 y1])))
      (first)))

(defn orientation
 "Orientation of p in regard to line a-b.
 If p is to the 'left' of the line the function return -1.
 If p is to the 'right' - return 1.
 If p is on the same line - return 0."
 [a b p]
 {:pre [(s/assert :specs/cell a) (s/assert :specs/cell b) (s/assert :specs/cell p)]}
 (let [x1 (:x a) y1 (:y a)
       x2 (:x b) y2 (:y b)
       x (:x p) y (:y p)]
   (-
     (* (- x x1) (- y2 y1))
     (* (- y y1) (- x2 x1)))))

(defn on-right-or-same-side?
 [a b p]
 (<= 0 (orientation a b p)))

(defn all-on-right-side?
 "Returns true if all the points in coll are to the 'right' of the a-b line.
 Target points may be on the same line to return true."
 [a b coll]
 (let [f (partial orientation a b)]
   (every? #(<= 0 (f %)) coll)))

(defn all-on-left-side?
 "Returns true if all the points in coll are to the 'left' of the a-b line.
 Target points may be on the same line to return true."
 [a b coll]
 (let [f (partial orientation a b)]
   (every? #(>= 0 (f %)) coll)))

(defn active-range
 "Helper function return range of numbers from p to N, where N = length of v."
 [v p]
 (range p (count v)))

(defn candidates
 "Returns cells that meet two criteria:
 1. They are in the active range (range of indexes provided by active-range).
 2. They are all close to p (not further than 1 cell away in any direction.)

 If there is nothing to return the result is the previous index (endge cases)."
 [v p]
 (let [res (filter #(nearest-cell? (v p) (v %)) (active-range v p))]
   (if (empty? res) (list (dec p)) res)))

(defn scan-zone
 "Returns a vector of points that should be checked whether they are to the
 left or to the right of the specified line.
 Based on the active-range results but instead of indexes returns the actual
 point objects."
 [v p]
 (let [r (active-range v p)]
  (subvec v (first r) (inc (last r)))))

(defn candidates-alt
 [v p aux-cond]
 (keep-indexed
  #(when
    (and
     (nearest-cell? (v p) %2)
     (aux-cond %2 (v p)))
    %1)
  v))

(defn find-next
 [v p f aux-cond prev]
 #?(:clj (proto-repl.saved-values/save 1))
 (reduce
  #(if (f (v p) (v %2) (scan-zone v p)) %2 %1)
  (remove #(= prev %) (candidates-alt v p aux-cond))))

(def top-only
 (fn [c1 c2]
  (and
   (>= (:y c1) (:y c2)))))

(def right-only
 (fn [c1 c2]
  (and
   (>= (:x c1) (:x c2)))))

(def top-left-only
 (fn [c1 c2]
  (and
   (>= (:y c1) (:y c2))
   (<= (:x c1) (:x c2)))))

(def top-right-only
 (fn [c1 c2]
  (and
   (>= (:y c1) (:y c2))
   (>= (:x c1) (:x c2)))))

(def right-bottom-only
 (fn [c1 c2]
  (and
   (>= (:x c1) (:x c2))
   (<= (:y c1) (:y c2)))))

(def right-top-only
 (fn [c1 c2]
  (and
   (>= (:x c1) (:x c2))
   (>= (:y c1) (:y c2)))))

(defn coord [{:keys [x y]}] [x y])

(defn outline-recur
 [v start f aux-cond]
 (loop [res [start] p start prev start]
  #?(:clj (proto-repl.saved-values/save 2))
  (let [np (find-next v p f aux-cond prev)]
    (if (= np (dec (count v)))
     (conj res np)
     (recur (conj res np) np p)))))

(defn outline->walls
 [v idx]
 (reduce
  (fn [res [a b]]
   (conj res
    {:src (coord (get v a))
     :dst (coord (get v b))}))
  []
  (partition 2 1 (idx))))

(defn vertical-shape?
 "Returns true if the x-range is less than y-range.
  Importante: highly dependable on the sorting."
 [v]
 (let [x-min (:x (first v)) y-min (:y (first v))
       x-max (:x (last v)) y-max (:y (last v))]
   (< (- x-max x-min) (- y-max y-min))))

(defn get-start
 ([v] (get-start v (vertical-shape? v)))
 ([v vertical?]
  (if vertical? (bottom-most-ind v) (left-most-ind v))))

(defn walls-around
 ([v]
  (let [vertical? (vertical-shape? v)]
   (walls-around v (get-start v vertical?) vertical?)))
 ([v start vertical?]
  (let [aux-cond-left (if vertical? top-only right-only)
        aux-cond-right (if vertical? top-only right-only)]
    (concat
      (->>
        (outline-recur v start all-on-right-side? aux-cond-left)
        (outline->walls v))
      (->>
       (outline-recur v start all-on-left-side? aux-cond-right)
       (outline->walls v))))))

(defn close-to-any?
 [coll x]
 (not
  (empty?
    (filter #(or (same-cell-coord? x %) (nearest-cell? x %)) coll))))

(defn add-if-close
 [coll x]
 (if (close-to-any? coll x)
   [(conj coll x)]
   [coll]))

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

(defn sort-fn-for
 [coll]
 (if (vertical-shape? coll) (juxt :y :x) (juxt :x :y)))

(defn sort-walls
 [walls]
 (sort-by (sort-fn-for walls) walls))






;;;; New world!

(def funky-walls
 '({:x 7, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 6, :y 3, :surrounded false, :status :wall, :player :red}
   {:x 6, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 9, :y 3, :surrounded false, :status :wall, :player :red}
   {:x 7, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 10, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 11, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 5, :surrounded false, :status :wall, :player :red}
   {:x 9, :y 5, :surrounded false, :status :wall, :player :red}
   {:x 12, :y 5, :surrounded false, :status :wall, :player :red}
   {:x 10, :y 6, :surrounded false, :status :wall, :player :red}
   {:x 11, :y 6, :surrounded false, :status :wall, :player :red}))



(defn horizontally-most
 [coll pred]
 (reduce
       (fn [{x1 :x y1 :y :as c1} {x2 :x y2 :y :as c2}]
         (cond
          (= x1 x2) (if (< y1 y2) c1 c2)
          (pred x1 x2) c1
          :else c2))
      coll))

(defn left-most
 [coll]
 (horizontally-most coll <))

(defn right-most
 [coll]
 (horizontally-most coll >))

(defn left-to-right?
 [[x1 _] [x2 _]]
 (>= x2 x1))


(defn right-candidates
 [coll {x1 :x y1 :y :as start}]
 (filter (fn [{x2 :x y2 :y :as next-cell}]
           (and (nearest-cell? start next-cell)
                (left-to-right? [x1 y1] [x2 y2])))
         coll))

(defn select-next-top
 [{:keys [x y] :as current} colls]
 (let [same-x (-> (filter #(= x (:x %)) colls) first)]
   (if (and (not (nil? same-x))
            (> (:y same-x) y))
       same-x
       (apply max-key :y colls))))

(defn select-next-bottom
 [{:keys [x y] :as current} colls]
 (let [same-x (-> (filter #(= x (:x %)) colls) first)]
   (if (and (not (nil? same-x))
            (< (:y same-x) y))
       same-x
       (apply min-key :y colls))))

(defn outline-cells->walls
 [outline]
 (reduce
   (fn [res [a b]]
    (conj res
     {:src (coord a)
      :dst (coord b)}))
   []
   (partition 2 1 outline)))


(defn draw-outline
 [coll select-next-fn]
 (loop [coll coll current (left-most coll) outline (list current)]
  (let [candidates (right-candidates coll current)]
    (if (or (= current (right-most coll)) (empty? candidates))
      outline
      (let [next (select-next-fn current candidates)]
        (recur coll next (conj outline next)))))))

(defn draw-walls
  [coll]
  (let [draw (partial draw-outline coll)]
    (concat
      (-> (draw select-next-bottom) (outline-cells->walls))
      (-> (draw select-next-top) (outline-cells->walls)))))

(defn get-walls
 [board player]
 (let [walls (walls-of board player)]
   (if (empty? walls)
     (list)
     (->> walls
          (sort-walls)
          (walls->clusters)
          (map draw-walls)))))

(defn walls-for-game
 [game]
 (let [board (:board game)
       red-walls (get-walls board :red)
       blue-walls (get-walls board :blue)]
   (assoc-in game [:walls] {:red red-walls :blue blue-walls})))
;
;
; (def walls-test
;  '({:y 2, :surrounded false, :status :wall, :player :red, :x 1}
;    {:y 2, :surrounded false, :status :wall, :player :red, :x 2}
;    {:y 3, :surrounded false, :status :wall, :player :red, :x 0}
;    {:y 3, :surrounded false, :status :wall, :player :red, :x 2}
;    {:y 4, :surrounded false, :status :wall, :player :red, :x 1}))
;
; (defn draw-outline
;  [coll select-next-fn]
;  (loop [coll coll current (left-most coll) outline (list current)]
;   (let [candidates (right-candidates coll current)]
;     (if (or (= current (right-most coll)) (empty? candidates))
;       outline
;       (let [next (select-next-fn current candidates)]
;         (recur coll next (conj outline next)))))))
;
; (draw-outline walls-test select-next-bottom)
; (draw-outline walls-test select-next-top)
;
; (draw-outline funky-walls select-next-bottom)
; (draw-outline funky-walls select-next-top)
;
; (def start (left-most walls-test))
; (->> (right-candidates walls-test start)
;      (select-next-bottom start))
;
;
; (loop [coll funky-walls current (left-most funky-walls) outline (list current) count 0]
;  (if (> 4 count)
;   (let [candidates (right-candidates coll current)]
;     (if (or (= current (right-most coll)) (empty? candidates))
;       outline
;       (let [next (select-next-bottom current candidates)]
;         (println next)
;         (recur coll next (conj outline next) (inc count)))))
;   (println "bang")))
;
; (def bad {:y 3, :surrounded false, :status :wall, :player :red, :x 2})
; (right-candidates walls-test bad)
; (def candidates '({:y 3, :surrounded false, :status :wall, :player :red, :x 2}))
; (select-next-bottom bad candidates)
;
; (let [same-x (-> (filter #(= 2 (:x %)) candidates) first)]
;    (if (and (not (nil? same-x))
;             (< (:y same-x) 2))
;        same-x
;        (apply min-key :y (conj candidates bad))))
;
; (def sx {:y 3, :surrounded false, :status :wall, :player :red, :x 2})
; (and (not (nil? sx)) (> (:y sx) 2))
