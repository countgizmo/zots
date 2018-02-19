(ns clj.zots.cluster-test
 (:require [clojure.test :refer :all])
 (:require [cljc.zots.cluster :refer [collect-clusters]]))

(def big-and-small-data
  [[4 1] [2 2] [3 2] [5 2] [1 3] [6 3] [2 4] [5 4] [3 5] [4 5]
   [7 1] [8 0] [9 1] [8 2]])

(def vertical-diamond-data
  [[7 3] [9 3] [8 4]
   [7 1] [8 0] [9 1] [8 2]])

(def vertical-spread-data
  [[7 6] [9 6] [8 7] [8 5]
   [7 1] [8 0] [9 1] [8 2]])

(deftest big-and-small
  (let [clusters (collect-clusters big-and-small-data)]
    (is (= 2 (count clusters)))))

(deftest vertical-diamond
  (let [clusters (collect-clusters vertical-diamond-data)]
    (is (= 1 (count clusters)))))

(deftest vertical-spread
  (let [clusters (collect-clusters vertical-spread-data)]
    (is (= 2 (count clusters)))))
