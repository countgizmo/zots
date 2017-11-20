(ns clj.zots.wall
 (:require [clj.zots.debug-printer :as dp]))

(defn add-vertex
 [graph {:keys [x y]} conns]
 (if (nil? (get graph [x y]))
   (assoc graph [x y] conns)
   graph))

(defn nearest-wall?
 [x y x1 y1]
 (if (and (= x x1) (= y y1))
   false
   (and (>= 1 (Math/abs (- x x1)))
        (>= 1 (Math/abs (- y y1))))))

(defn find-connections
 [walls {:keys [x y]}]
 (filter #(nearest-wall? x y (:x %) (:y %)) walls))

(defn build-graph
 [walls]
 (reduce
   (fn [res c] (add-vertex res c (find-connections walls c))) {} walls))

(defn walls-of
 [state player]
 (filter
   #(and (= player :player %) (= :wall (:status %)))
   (flatten (:board state))))

(defn get-walls
 [state player]
 (as-> (walls-of state player) w
   (build-graph w)
   {:walls w :player player}))
