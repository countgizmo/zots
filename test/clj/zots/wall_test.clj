(ns clj.zots.wall-test
 (:require [clojure.test :refer :all]
           [cljc.zots.wall :as wall]
           [cljc.zots.specs :refer :all]))

(deftest same-cell-coord-test-true
 (let [c1 {:x 1 :y 2 :status :wall :surrounded false :player :red}
       c2 {:x 1 :y 2 :status :active :surrounded true :player :blue}]
  (is (true? (wall/same-cell-coord? c1 c2)))))
