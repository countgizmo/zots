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
 [[{:x 0, :y 0, :surrounded false, :status :active, :player :none}
   {:x 1, :y 0, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 0, :surrounded false, :status :active, :player :none}]
  [{:x 0, :y 1, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 1, :surrounded true, :status :active, :player :blue}
   {:x 2, :y 1, :surrounded false, :status :wall, :player :red}]
  [{:x 0, :y 2, :surrounded false, :status :active, :player :none}
   {:x 1, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 2, :surrounded false, :status :active, :player :none}]])

(def diamond-wall
 '(({:dst [1 0] :src [0 1]}
    {:dst [0 1] :src [1 2]}
    {:dst [1 2] :src [2 1]}
    {:dst [2 1] :src [1 0]})))

(def square
 [[{:x 0, :y 0, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 0, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 0, :surrounded false, :status :wall, :player :red}]
  [{:x 0, :y 1, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 1, :surrounded true, :status :active, :player :blue}
   {:x 2, :y 1, :surrounded false, :status :wall, :player :red}]
  [{:x 0, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 2, :surrounded false, :status :wall, :player :red}]])

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
 [[{:x 0, :y 0, :surrounded false, :status :active, :player :none}
   {:x 1, :y 0, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 0, :surrounded false, :status :active, :player :none}]
  [{:x 0, :y 1, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 1, :surrounded true, :status :active, :player :blue}
   {:x 2, :y 1, :surrounded false, :status :wall, :player :red}]
  [{:x 0, :y 2, :surrounded false, :status :active, :player :none}
   {:x 1, :y 2, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 2, :surrounded false, :status :active, :player :none}]
  [{:x 0, :y 3, :surrounded false, :status :wall, :player :red}
   {:x 1, :y 3, :surrounded true, :status :active, :player :blue}
   {:x 2, :y 3, :surrounded false, :status :wall, :player :red}]
  [{:x 0, :y 4, :surrounded false, :status :active, :player :none}
   {:x 1, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 2, :y 4, :surrounded false, :status :active, :player :none}]])

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
  [[{:x 0, :y 0, :surrounded false, :status :active, :player :none}
    {:x 1, :y 0, :surrounded false, :status :active, :player :none}
    {:x 2, :y 0, :surrounded false, :status :active, :player :none}
    {:x 3, :y 0, :surrounded false, :status :active, :player :none}
    {:x 4, :y 0, :surrounded false, :status :active, :player :none}
    {:x 5, :y 0, :surrounded false, :status :active, :player :none}
    {:x 6, :y 0, :surrounded false, :status :active, :player :none}
    {:x 7, :y 0, :surrounded false, :status :active, :player :none}
    {:x 8, :y 0, :surrounded false, :status :active, :player :none}
    {:x 9, :y 0, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 1, :surrounded false, :status :active, :player :none}
    {:x 1, :y 1, :surrounded false, :status :active, :player :none}
    {:x 2, :y 1, :surrounded false, :status :active, :player :none}
    {:x 3, :y 1, :surrounded false, :status :active, :player :none}
    {:x 4, :y 1, :surrounded false, :status :active, :player :none}
    {:x 5, :y 1, :surrounded false, :status :active, :player :none}
    {:x 6, :y 1, :surrounded false, :status :active, :player :none}
    {:x 7, :y 1, :surrounded false, :status :active, :player :none}
    {:x 8, :y 1, :surrounded false, :status :active, :player :none}
    {:x 9, :y 1, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 2, :surrounded false, :status :active, :player :none}
    {:x 1, :y 2, :surrounded false, :status :active, :player :none}
    {:x 2, :y 2, :surrounded false, :status :active, :player :none}
    {:x 3, :y 2, :surrounded false, :status :active, :player :none}
    {:x 4, :y 2, :surrounded false, :status :active, :player :none}
    {:x 5, :y 2, :surrounded false, :status :active, :player :none}
    {:x 6, :y 2, :surrounded false, :status :wall, :player :blue}
    {:x 7, :y 2, :surrounded false, :status :active, :player :none}
    {:x 8, :y 2, :surrounded false, :status :active, :player :none}
    {:x 9, :y 2, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 3, :surrounded false, :status :active, :player :none}
    {:x 1, :y 3, :surrounded false, :status :active, :player :none}
    {:x 2, :y 3, :surrounded false, :status :active, :player :none}
    {:x 3, :y 3, :surrounded false, :status :active, :player :none}
    {:x 4, :y 3, :surrounded false, :status :active, :player :none}
    {:x 5, :y 3, :surrounded false, :status :wall, :player :blue}
    {:x 6, :y 3, :surrounded true, :status :active, :player :red}
    {:x 7, :y 3, :surrounded false, :status :wall, :player :blue}
    {:x 8, :y 3, :surrounded false, :status :active, :player :none}
    {:x 9, :y 3, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 4, :surrounded false, :status :active, :player :none}
    {:x 1, :y 4, :surrounded false, :status :active, :player :none}
    {:x 2, :y 4, :surrounded false, :status :active, :player :none}
    {:x 3, :y 4, :surrounded false, :status :active, :player :none}
    {:x 4, :y 4, :surrounded false, :status :active, :player :none}
    {:x 5, :y 4, :surrounded false, :status :active, :player :none}
    {:x 6, :y 4, :surrounded false, :status :wall, :player :blue}
    {:x 7, :y 4, :surrounded true, :status :active, :player :red}
    {:x 8, :y 4, :surrounded false, :status :wall, :player :blue}
    {:x 9, :y 4, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 5, :surrounded false, :status :active, :player :none}
    {:x 1, :y 5, :surrounded false, :status :active, :player :none}
    {:x 2, :y 5, :surrounded false, :status :active, :player :none}
    {:x 3, :y 5, :surrounded false, :status :active, :player :none}
    {:x 4, :y 5, :surrounded false, :status :active, :player :none}
    {:x 5, :y 5, :surrounded false, :status :wall, :player :red}
    {:x 6, :y 5, :surrounded false, :status :wall, :player :red}
    {:x 7, :y 5, :surrounded false, :status :wall, :player :blue}
    {:x 8, :y 5, :surrounded true, :status :active, :player :red}
    {:x 9, :y 5, :surrounded false, :status :wall, :player :blue}]
   [{:x 0, :y 6, :surrounded false, :status :active, :player :none}
    {:x 1, :y 6, :surrounded false, :status :active, :player :none}
    {:x 2, :y 6, :surrounded false, :status :active, :player :none}
    {:x 3, :y 6, :surrounded false, :status :active, :player :none}
    {:x 4, :y 6, :surrounded false, :status :wall, :player :red}
    {:x 5, :y 6, :surrounded true, :status :active, :player :blue}
    {:x 6, :y 6, :surrounded true, :status :active, :player :blue}
    {:x 7, :y 6, :surrounded false, :status :wall, :player :red}
    {:x 8, :y 6, :surrounded false, :status :wall, :player :blue}
    {:x 9, :y 6, :surrounded false, :status :active, :player :none}]
   [{:x 0, :y 7, :surrounded false, :status :active, :player :none}
    {:x 1, :y 7, :surrounded false, :status :active, :player :none}
    {:x 2, :y 7, :surrounded false, :status :active, :player :none}
    {:x 3, :y 7, :surrounded false, :status :active, :player :none}
    {:x 4, :y 7, :surrounded false, :status :active, :player :none}
    {:x 5, :y 7, :surrounded false, :status :wall, :player :red}
    {:x 6, :y 7, :surrounded false, :status :wall, :player :red}
    {:x 7, :y 7, :surrounded true, :status :active, :player :blue}
    {:x 8, :y 7, :surrounded false, :status :wall, :player :red}
    {:x 9, :y 7, :surrounded false, :status :active, :player :blue}]
   [{:x 0, :y 8, :surrounded false, :status :active, :player :none}
    {:x 1, :y 8, :surrounded false, :status :active, :player :none}
    {:x 2, :y 8, :surrounded false, :status :active, :player :none}
    {:x 3, :y 8, :surrounded false, :status :active, :player :none}
    {:x 4, :y 8, :surrounded false, :status :active, :player :none}
    {:x 5, :y 8, :surrounded false, :status :wall, :player :red}
    {:x 6, :y 8, :surrounded true, :status :active, :player :blue}
    {:x 7, :y 8, :surrounded false, :status :wall, :player :red}
    {:x 8, :y 8, :surrounded true, :status :active, :player :blue}
    {:x 9, :y 8, :surrounded false, :status :wall, :player :red}]
   [{:x 0, :y 9, :surrounded false, :status :active, :player :none}
    {:x 1, :y 9, :surrounded false, :status :active, :player :none}
    {:x 2, :y 9, :surrounded false, :status :active, :player :none}
    {:x 3, :y 9, :surrounded false, :status :active, :player :none}
    {:x 4, :y 9, :surrounded false, :status :active, :player :none}
    {:x 5, :y 9, :surrounded false, :status :wall, :player :red}
    {:x 6, :y 9, :surrounded true, :status :active, :player :blue}
    {:x 7, :y 9, :surrounded false, :status :wall, :player :red}
    {:x 8, :y 9, :surrounded false, :status :wall, :player :red}
    {:x 9, :y 9, :surrounded false, :status :active, :player :blue}]
   [{:x 0, :y 10, :surrounded false, :status :active, :player :none}
    {:x 1, :y 10, :surrounded false, :status :active, :player :none}
    {:x 2, :y 10, :surrounded false, :status :active, :player :none}
    {:x 3, :y 10, :surrounded false, :status :active, :player :none}
    {:x 4, :y 10, :surrounded false, :status :active, :player :none}
    {:x 5, :y 10, :surrounded false, :status :active, :player :none}
    {:x 6, :y 10, :surrounded false, :status :wall, :player :red}
    {:x 7, :y 10, :surrounded false, :status :active, :player :none}
    {:x 8, :y 10, :surrounded false, :status :active, :player :none}
    {:x 9, :y 10, :surrounded false, :status :active, :player :none}]])

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
 [[{:x 6, :y 3, :surrounded false, :status :active, :player :none}
   {:x 7, :y 3, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 3, :surrounded false, :status :active, :player :none}]
  [{:x 6, :y 4, :surrounded false, :status :wall, :player :red}
   {:x 7, :y 4, :surrounded true, :status :active, :player :blue}
   {:x 8, :y 4, :surrounded false, :status :wall, :player :red}]
  [{:x 6, :y 5, :surrounded false, :status :active, :player :none}
   {:x 7, :y 5, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 5, :surrounded false, :status :active, :player :none}]
  [{:x 6, :y 6, :surrounded false, :status :active, :player :none}
   {:x 7, :y 6, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 6, :surrounded false, :status :active, :player :none}]
  [{:x 6, :y 7, :surrounded false, :status :wall, :player :red}
   {:x 7, :y 7, :surrounded true, :status :active, :player :blue}
   {:x 8, :y 7, :surrounded false, :status :wall, :player :red}]
  [{:x 6, :y 8, :surrounded false, :status :active, :player :none}
   {:x 7, :y 8, :surrounded false, :status :wall, :player :red}
   {:x 8, :y 8, :surrounded false, :status :active, :player :none}]])

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
