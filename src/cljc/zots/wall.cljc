(ns cljc.zots.wall
  (:require [clojure.spec.alpha :as s]
            [cljc.zots.specs :as specs]
            [cljc.zots.concave-hull :refer [concave-hull]]
            [cljc.zots.board :as board]
            [cljc.zots.cluster :refer [collect-clusters]]
            #?(:clj [proto-repl.saved-values])))

(defn walls-of
 "Return all the walls belonging to specified player."
 [board player]
 {:pre [(s/assert ::specs/player player)]}
 (map first
   (filter
     (fn [[k v]] (and (= player (:player v)) (= :wall (:status v))))
     board)))

(defn outline->walls
 [outline]
 (reduce
   (fn [res [a b]]
    (conj res
     {:src a
      :dst b}))
   ()
   (partition 2 1 outline)))

(defn get-walls
 [board player]
 (let [walls (walls-of board player)]
   (if (empty? walls)
     (list)
     (->> walls
          (collect-clusters)
          (map concave-hull)
          (map outline->walls)))))

(defn walls-for-game
 [game]
 (let [board (:board game)
       red-walls (get-walls board :red)
       blue-walls (get-walls board :blue)]
   (assoc-in game [:walls] {:red red-walls :blue blue-walls})))
