(ns clj.zots.wall-test
 (:require [clojure.test :refer :all])
 (:require [cljc.zots.wall :as wall]))

(deftest same-cell-coord-test-true
 (let [c1 {:x 1 :y 2}
       c2 {:x 1 :y 2}]
  (is (true? (wall/same-cell-coord? c1 c2)))))
