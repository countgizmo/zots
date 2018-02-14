(ns cljc.zots.concave-hull)

(defn find-min-y
 "Point with the min y coordinate.
  In case of a tie compare x and select the one with the lowest value."
 [coll]
 (reduce
   (fn [[x1 y1 :as p1] [x2 y2 :as p2]]
     (cond
      (= y1 y2) (if (< x1 x2) p1 p2)
      (< y1 y2) p1
      :else p2))
   coll))

(defn angle-from-xx
 [[x1 y1] [x2 y2]]
 (if (= x1 x2)
   0
   (-> (/ (- y2 y1) (- x2 x1))
       (Math/atan))))

(defn orientation
 "Orientation of p (x2 y2) in regard to line a (x0 y0) - b (x1 y1).
 If (x2 y2) is to the 'left' of the line if return is < 0.
 If (x2 y2) is to the 'right' - return is > 0;
 If (x2 y2) is on the same line - return is 0."
 [[x0 y0] [x1 y1] [x2 y2]]
 (-
   (* (- x0 x1) (- y2 y1))
   (* (- y0 y1) (- x2 x1))))

(defn nearest-cell?
 [[x1 y1] [x2 y2]]
 (if (and (= x1 x2) (= y1 y2))
   false
   (and (>= 1 (Math/abs (- x1 x2)))
        (>= 1 (Math/abs (- y1 y2))))))

(defn neighbors
 [from coll]
 (filter (partial nearest-cell? from) coll))

(defn find-next
 [[x1 y1 :as from] coll cond-fn]
 (when-let [candidates (neighbors from coll)]
   (-> (sort-by (comp - (partial cond-fn from)) candidates)
       (first))))

(defn add-later?
 [p coll]
 (< 2 (count (neighbors p coll))))

(defn concave-hull
 [coll-orig]
 (let [p1 (find-min-y coll-orig)
       p2 (find-next p1 coll-orig angle-from-xx)
       walls [p1 p2]]
   (loop [prev p1 current p2 coll coll-orig walls walls add-later [p1]]
     (let [new-coll (remove #(= % prev) coll)
           next (find-next current new-coll (partial orientation prev))
           later (if (add-later? prev coll-orig) [prev] [])]
       (if (= current p1)
         walls
         (recur
          current
          next
          (into new-coll add-later)
          (conj walls next)
          later))))))
