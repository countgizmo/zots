(ns cljc.zots.geometry)

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

(defn nearest-cell?
 [[x1 y1] [x2 y2]]
 (if (and (= x1 x2) (= y1 y2))
   false
   (and (>= 1 (Math/abs (- x1 x2)))
        (>= 1 (Math/abs (- y1 y2))))))

(defn neighbors
 [from coll]
 (filter (partial nearest-cell? from) coll))

(defn nearest-coll?
 [coll p]
 (true? (some #(nearest-cell? p %) coll)))

(defn neighbors-all
 [from-coll coll]
 (filter (partial nearest-coll? from-coll) coll))
