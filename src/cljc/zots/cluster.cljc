(ns cljc.zots.cluster
  (:require [cljc.zots.geometry :refer [find-min-y neighbors neighbors-all]]))

(defn collect-clusters
 ([coll-orig] (collect-clusters coll-orig ()))
 ([coll-orig clusters]
  (let [start (find-min-y coll-orig)
        [clusters coll] (loop [coll coll-orig ps (neighbors start coll-orig) cluster ()]
                          (if (empty? ps)
                            [(conj clusters cluster) coll]
                            (let [next-set (set (neighbors-all ps coll))
                                  next-coll (remove #(contains? next-set %) coll)
                                  cluster (into cluster next-set)]
                              (recur next-coll next-set cluster))))]
    (if (empty? coll) clusters (recur coll clusters)))))
