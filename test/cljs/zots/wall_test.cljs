(ns cljs.zots.wall-test
 (:require [cljs.test :refer-macros [deftest is testing run-tests]])
 (:require [cljs.zots.wall :as wall]))

(deftest same-cell-coord-test-true
 (let [c1 {:x 1 :y 2}
       c2 {:x 1 :y 2}]
  (is (true? (wall/same-cell-coord? c1 c2)))))


(cljs.test/run-tests)
