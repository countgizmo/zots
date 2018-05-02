(ns clj.zots.wall-test
  (:require [clojure.test :refer :all]
            [cljc.zots.wall :as wall]
            [cljc.zots.board :as board]
            [cljc.zots.specs :as specs]
            [clojure.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def diamond
 {[0 0] {:surrounded? false, :status :active, :player :none}
  [1 0] {:surrounded? false, :status :wall, :player :red}
  [2 0] {:surrounded? false, :status :active, :player :none}
  [0 1] {:surrounded? false, :status :wall, :player :red}
  [1 1] {:surrounded? true, :status :active, :player :blue}
  [2 1] {:surrounded? false, :status :wall, :player :red}
  [0 2] {:surrounded? false, :status :active, :player :none}
  [1 2] {:surrounded? false, :status :wall, :player :red}
  [2 2] {:surrounded? false, :status :active, :player :none}})

(def diamond-wall
 '(({:dst [1 0] :src [0 1]}
    {:dst [0 1] :src [1 2]}
    {:dst [1 2] :src [2 1]}
    {:dst [2 1] :src [1 0]})))

(def square
 {[0 0] {:surrounded? false, :status :wall, :player :red}
  [1 0] {:surrounded? false, :status :wall, :player :red}
  [2 0] {:surrounded? false, :status :wall, :player :red}
  [0 1] {:surrounded? false, :status :wall, :player :red}
  [1 1] {:surrounded? true, :status :active, :player :blue}
  [2 1] {:surrounded? false, :status :wall, :player :red}
  [0 2] {:surrounded? false, :status :wall, :player :red}
  [1 2] {:surrounded? false, :status :wall, :player :red}
  [2 2] {:surrounded? false, :status :wall, :player :red}})

(def square-wall
 '(({:dst [0 0] :src [0 1]}
    {:dst [0 1] :src [0 2]}
    {:dst [0 2] :src [1 2]}
    {:dst [1 2] :src [2 2]}
    {:dst [2 2] :src [2 1]}
    {:dst [2 1] :src [2 0]}
    {:dst [2 0] :src [1 0]}
    {:dst [1 0] :src [0 0]})))

(def vertical-eight
 {[0 0] {:surrounded? false, :status :active, :player :none}
  [1 0] {:surrounded? false, :status :wall, :player :red}
  [2 0] {:surrounded? false, :status :active, :player :none}
  [0 1] {:surrounded? false, :status :wall, :player :red}
  [1 1] {:surrounded? true, :status :active, :player :blue}
  [2 1] {:surrounded? false, :status :wall, :player :red}
  [0 2] {:surrounded? false, :status :active, :player :none}
  [1 2] {:surrounded? false, :status :wall, :player :red}
  [2 2] {:surrounded? false, :status :active, :player :none}
  [0 3] {:surrounded? false, :status :wall, :player :red}
  [1 3] {:surrounded? true, :status :active, :player :blue}
  [2 3] {:surrounded? false, :status :wall, :player :red}
  [0 4] {:surrounded? false, :status :active, :player :none}
  [1 4] {:surrounded? false, :status :wall, :player :red}
  [2 4] {:surrounded? false, :status :active, :player :none}})

(def vertical-eight-wall-set
  #{'({:src [0 1] :dst [1 0]}
      {:src [1 2] :dst [0 1]}
      {:src [0 3] :dst [1 2]}
      {:src [1 4] :dst [0 3]}
      {:src [2 3] :dst [1 4]}
      {:src [1 2] :dst [2 3]}
      {:src [2 1] :dst [1 2]}
      {:src [1 0] :dst [2 1]})})

(def js-error-out-of-bounds
  {[0 0] {:surrounded? false, :status :active, :player :none}
   [1 0] {:surrounded? false, :status :active, :player :none}
   [2 0] {:surrounded? false, :status :active, :player :none}
   [3 0] {:surrounded? false, :status :active, :player :none}
   [4 0] {:surrounded? false, :status :active, :player :none}
   [5 0] {:surrounded? false, :status :active, :player :none}
   [6 0] {:surrounded? false, :status :active, :player :none}
   [7 0] {:surrounded? false, :status :active, :player :none}
   [8 0] {:surrounded? false, :status :active, :player :none}
   [9 0] {:surrounded? false, :status :active, :player :none}
   [0 1] {:surrounded? false, :status :active, :player :none}
   [1 1] {:surrounded? false, :status :active, :player :none}
   [2 1] {:surrounded? false, :status :active, :player :none}
   [3 1] {:surrounded? false, :status :active, :player :none}
   [4 1] {:surrounded? false, :status :active, :player :none}
   [5 1] {:surrounded? false, :status :active, :player :none}
   [6 1] {:surrounded? false, :status :active, :player :none}
   [7 1] {:surrounded? false, :status :active, :player :none}
   [8 1] {:surrounded? false, :status :active, :player :none}
   [9 1] {:surrounded? false, :status :active, :player :none}
   [0 2] {:surrounded? false, :status :active, :player :none}
   [1 2] {:surrounded? false, :status :active, :player :none}
   [2 2] {:surrounded? false, :status :active, :player :none}
   [3 2] {:surrounded? false, :status :active, :player :none}
   [4 2] {:surrounded? false, :status :active, :player :none}
   [5 2] {:surrounded? false, :status :active, :player :none}
   [6 2] {:surrounded? false, :status :wall, :player :blue}
   [7 2] {:surrounded? false, :status :active, :player :none}
   [8 2] {:surrounded? false, :status :active, :player :none}
   [9 2] {:surrounded? false, :status :active, :player :none}
   [0 3] {:surrounded? false, :status :active, :player :none}
   [1 3] {:surrounded? false, :status :active, :player :none}
   [2 3] {:surrounded? false, :status :active, :player :none}
   [3 3] {:surrounded? false, :status :active, :player :none}
   [4 3] {:surrounded? false, :status :active, :player :none}
   [5 3] {:surrounded? false, :status :wall, :player :blue}
   [6 3] {:surrounded? true, :status :active, :player :red}
   [7 3] {:surrounded? false, :status :wall, :player :blue}
   [8 3] {:surrounded? false, :status :active, :player :none}
   [9 3] {:surrounded? false, :status :active, :player :none}
   [0 4] {:surrounded? false, :status :active, :player :none}
   [1 4] {:surrounded? false, :status :active, :player :none}
   [2 4] {:surrounded? false, :status :active, :player :none}
   [3 4] {:surrounded? false, :status :active, :player :none}
   [4 4] {:surrounded? false, :status :active, :player :none}
   [5 4] {:surrounded? false, :status :active, :player :none}
   [6 4] {:surrounded? false, :status :wall, :player :blue}
   [7 4] {:surrounded? true, :status :active, :player :red}
   [8 4] {:surrounded? false, :status :wall, :player :blue}
   [9 4] {:surrounded? false, :status :active, :player :none}
   [0 5] {:surrounded? false, :status :active, :player :none}
   [1 5] {:surrounded? false, :status :active, :player :none}
   [2 5] {:surrounded? false, :status :active, :player :none}
   [3 5] {:surrounded? false, :status :active, :player :none}
   [4 5] {:surrounded? false, :status :active, :player :none}
   [5 5] {:surrounded? false, :status :wall, :player :red}
   [6 5] {:surrounded? false, :status :wall, :player :red}
   [7 5] {:surrounded? false, :status :wall, :player :blue}
   [8 5] {:surrounded? true, :status :active, :player :red}
   [9 5] {:surrounded? false, :status :wall, :player :blue}
   [0 6] {:surrounded? false, :status :active, :player :none}
   [1 6] {:surrounded? false, :status :active, :player :none}
   [2 6] {:surrounded? false, :status :active, :player :none}
   [3 6] {:surrounded? false, :status :active, :player :none}
   [4 6] {:surrounded? false, :status :wall, :player :red}
   [5 6] {:surrounded? true, :status :active, :player :blue}
   [6 6] {:surrounded? true, :status :active, :player :blue}
   [7 6] {:surrounded? false, :status :wall, :player :red}
   [8 6] {:surrounded? false, :status :wall, :player :blue}
   [9 6] {:surrounded? false, :status :active, :player :none}
   [0 7] {:surrounded? false, :status :active, :player :none}
   [1 7] {:surrounded? false, :status :active, :player :none}
   [2 7] {:surrounded? false, :status :active, :player :none}
   [3 7] {:surrounded? false, :status :active, :player :none}
   [4 7] {:surrounded? false, :status :active, :player :none}
   [5 7] {:surrounded? false, :status :wall, :player :red}
   [6 7] {:surrounded? false, :status :wall, :player :red}
   [7 7] {:surrounded? true, :status :active, :player :blue}
   [8 7] {:surrounded? false, :status :wall, :player :red}
   [9 7] {:surrounded? false, :status :active, :player :blue}
   [0 8] {:surrounded? false, :status :active, :player :none}
   [1 8] {:surrounded? false, :status :active, :player :none}
   [2 8] {:surrounded? false, :status :active, :player :none}
   [3 8] {:surrounded? false, :status :active, :player :none}
   [4 8] {:surrounded? false, :status :active, :player :none}
   [5 8] {:surrounded? false, :status :wall, :player :red}
   [6 8] {:surrounded? true, :status :active, :player :blue}
   [7 8] {:surrounded? false, :status :wall, :player :red}
   [8 8] {:surrounded? true, :status :active, :player :blue}
   [9 8] {:surrounded? false, :status :wall, :player :red}
   [0 9] {:surrounded? false, :status :active, :player :none}
   [1 9] {:surrounded? false, :status :active, :player :none}
   [2 9] {:surrounded? false, :status :active, :player :none}
   [3 9] {:surrounded? false, :status :active, :player :none}
   [4 9] {:surrounded? false, :status :active, :player :none}
   [5 9] {:surrounded? false, :status :wall, :player :red}
   [6 9] {:surrounded? true, :status :active, :player :blue}
   [7 9] {:surrounded? false, :status :wall, :player :red}
   [8 9] {:surrounded? false, :status :wall, :player :red}
   [9 9] {:surrounded? false, :status :active, :player :blue}
   [0 10] {:surrounded? false, :status :active, :player :none}
   [1 10] {:surrounded? false, :status :active, :player :none}
   [2 10] {:surrounded? false, :status :active, :player :none}
   [3 10] {:surrounded? false, :status :active, :player :none}
   [4 10] {:surrounded? false, :status :active, :player :none}
   [5 10] {:surrounded? false, :status :active, :player :none}
   [6 10] {:surrounded? false, :status :wall, :player :red}
   [7 10] {:surrounded? false, :status :active, :player :none}
   [8 10] {:surrounded? false, :status :active, :player :none}
   [9 10] {:surrounded? false, :status :active, :player :none}})

(def js-error-out-of-bounds-walls
  '(({:dst [5 5] :src [4 6]}
     {:dst [4 6] :src [5 7]}
     {:dst [5 7] :src [5 8]}
     {:dst [5 8] :src [5 9]}
     {:dst [5 9] :src [6 10]}
     {:dst [6 10] :src [7 9]}
     {:dst [7 9] :src [8 9]}
     {:dst [8 9] :src [9 8]}
     {:dst [9 8] :src [8 7]}
     {:dst [8 7] :src [7 6]}
     {:dst [7 6] :src [6 5]}
     {:dst [6 5] :src [5 5]})))

(def js-error-out-of-bounds-2
 {[6 3] {:surrounded? false, :status :active, :player :none}
  [7 3] {:surrounded? false, :status :wall, :player :red}
  [8 3] {:surrounded? false, :status :active, :player :none}
  [6 4] {:surrounded? false, :status :wall, :player :red}
  [7 4] {:surrounded? true, :status :active, :player :blue}
  [8 4] {:surrounded? false, :status :wall, :player :red}
  [6 5] {:surrounded? false, :status :active, :player :none}
  [7 5] {:surrounded? false, :status :wall, :player :red}
  [8 5] {:surrounded? false, :status :active, :player :none}
  [6 6] {:surrounded? false, :status :active, :player :none}
  [7 6] {:surrounded? false, :status :wall, :player :red}
  [8 6] {:surrounded? false, :status :active, :player :none}
  [6 7] {:surrounded? false, :status :wall, :player :red}
  [7 7] {:surrounded? true, :status :active, :player :blue}
  [8 7] {:surrounded? false, :status :wall, :player :red}
  [6 8] {:surrounded? false, :status :active, :player :none}
  [7 8] {:surrounded? false, :status :wall, :player :red}
  [8 8] {:surrounded? false, :status :active, :player :none}})

(def js-error-out-of-bounds-2-walls
  '(({:dst [7 3] :src [6 4]}
     {:dst [6 4] :src [7 5]}
     {:dst [7 5] :src [7 6]}
     {:dst [7 6] :src [6 7]}
     {:dst [6 7] :src [7 8]}
     {:dst [7 8] :src [8 7]}
     {:dst [8 7] :src [7 6]}
     {:dst [7 6] :src [7 5]}
     {:dst [7 5] :src [8 4]}
     {:dst [8 4] :src [7 3]})))

(deftest wall-of-zero-walls-found
 (is (empty? (wall/walls-of diamond :blue))))

(deftest wall-of-many-walls-found
 (is (= 4 (count (wall/walls-of diamond :red)))))

(deftest diamond-shape-wall
 (is (= (set diamond-wall) (set (wall/get-walls diamond :red)))))

(deftest square-shape-wall
 (is (= (set square-wall) (set (wall/get-walls square :red)))))

(deftest vertical-eight-shape-wall
  (is (= vertical-eight-wall-set (set (wall/get-walls vertical-eight :red)))))

(deftest js-error-out-of-bounds-walls-check
 (is (= js-error-out-of-bounds-walls (wall/get-walls js-error-out-of-bounds :red))))

(deftest js-error-out-of-bounds-2-walls-check
 (is (= js-error-out-of-bounds-2-walls (wall/get-walls js-error-out-of-bounds-2 :red))))
