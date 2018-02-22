(ns clj.zots.concave-hull-test
 (:require [clojure.test :refer :all])
 (:require [cljc.zots.concave-hull :refer [concave-hull]]))

(def test-data-single
 [[0 1] [1 0] [2 1] [1 2]])

(def test-data-diamond
 [[0 1] [1 0] [2 1] [1 2]
  [3 2] [3 0] [4 1]])

(def test-data-dumbbell
 [[0 1] [1 0] [2 1] [1 2]
  [3 1] [4 2] [4 0] [5 1]])

(def test-data-katya
 [[4 7] [5 6] [4 9] [5 10] [6 10] [7 9] [8 10] [5 8]
  [9 10] [10 10] [11 9] [11 8] [10 7] [10 6] [9 5]
  [8 6] [8 7] [7 7] [6 6] [5 6]])

(def solution-katya
  [[9 5] [10 6] [10 7] [11 8] [11 9] [10 10] [9 10]
   [8 10] [7 9] [6 10] [5 10] [4 9] [5 8] [4 7] [5 6]
   [6 6] [7 7] [8 7] [8 6] [9 5]])

(def test-data-domik
 [[1 1] [1 2] [2 3] [3 2] [2 1]])

(deftest single
  (is (= [[1 0] [2 1] [1 2] [0 1] [1 0]]
         (concave-hull test-data-single))))

(deftest diamond
  (is (= (into #{} [[1 0] [2 1] [3 0] [4 1] [3 2] [2 1] [1 2] [0 1] [1 0]])
         (into #{} (concave-hull test-data-diamond)))))

(deftest dumbbell
  (is (= (into #{} [[1 0] [2 1] [3 1] [4 0] [5 1] [4 2] [3 1] [2 1] [1 2] [0 1] [1 0]])
         (into #{} (concave-hull test-data-dumbbell)))))

(deftest katya
  (is (= (into #{} solution-katya) (into #{} (concave-hull test-data-katya)))))

(deftest domik
 (is (= #{[2 3] [1 1] [2 1] [1 2] [3 2]})
     (concave-hull test-data-domik)))

(def three-diamonds-data
  [[1 2] [2 1] [4 1] [5 2] [4 3] [2 3] [3 4] [3 2]])

(deftest three-diamonds
 (is (= #{[2 1] [3 2] [4 1] [5 2] [4 3] [3 4] [2 3] [1 2]})
     (concave-hull three-diamonds-data)))
