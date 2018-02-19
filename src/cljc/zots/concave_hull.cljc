(ns cljc.zots.concave-hull
  (:require #?(:clj [proto-repl.saved-values])
            [cljc.zots.geometry :refer [find-min-y neighbors]]))

(defn angle-from-xx
 [[x1 y1] [x2 y2]]
 (* -1 (Math/atan2 (- y2 y1) (- x2 x1))))

(defn angle
 [[x0 y0] [x1 y1] [x2 y2]]
 (-
   (Math/atan2 (- y0 y1) (- x0 x1))
   (Math/atan2 (- y1 y2) (- x1 x2))))

(defn superangle
 [[x0 y0] [x1 y1] [x2 y2]]
 (let [a (+ (Math/pow (- x1 x0) 2) (Math/pow (- y1 y0) 2))
       b (+ (Math/pow (- x1 x2) 2) (Math/pow (- y1 y2) 2))
       c (+ (Math/pow (- x2 x0) 2) (Math/pow (- y2 y0) 2))
       z (- (+ a b) c)]
   (Math/acos (/ z (Math/sqrt (* 4 a b))))))

(defn orientation
 "Orientation of p (x2 y2) in regard to line a (x0 y0) - b (x1 y1).
 If (x2 y2) is to the 'left' of the line if return is < 0.
 If (x2 y2) is to the 'right' - return is > 0;
 If (x2 y2) is on the same line - return is 0."
 [[x0 y0] [x1 y1] [x2 y2]]
 (-
   (* (- x0 x1) (- y2 y1))
   (* (- y0 y1) (- x2 x1))))

(defn find-next
 ([[x1 y1 :as from] coll cond-fn]
  (when-let [candidates (neighbors from coll)]
    (->> (sort-by (comp - (partial cond-fn from)) candidates)
         (first))))
 ([[x1 y1 :as from] coll cond-fn cond-fn2]
  (when-let [candidates (neighbors from coll)]
    (->> (sort-by (comp - (partial cond-fn from)) candidates)
         (sort-by (comp - (partial cond-fn2 from)))
         (first)))))

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
           next (find-next current new-coll (partial orientation prev) (partial superangle prev))
           later (if (add-later? prev coll-orig) [prev] [])]
       (if (= current p1)
         walls
         (recur
          current
          next
          (into new-coll add-later)
          (conj walls next)
          later))))))
