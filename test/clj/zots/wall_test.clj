(ns clj.zots.wall-test
 (:require [clojure.test :refer :all]
           [cljc.zots.wall :as wall]
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
  '(({:src [1 0], :dst [0 1]}
     {:src [0 1], :dst [1 2]}
     {:src [1 0], :dst [2 1]}
     {:src [2 1], :dst [1 2]})))

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
 '(({:dst [0 1], :src [0 0]}
    {:dst [0 2], :src [0 1]}
    {:dst [1 2], :src [0 2]}
    {:dst [2 2], :src [1 2]}
    {:dst [1 0], :src [0 0]}
    {:dst [2 0], :src [1 0]}
    {:dst [2 1], :src [2 0]}
    {:dst [2 2], :src [2 1]})))

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

(def vertical-eight-wall
  '(({:dst [0 1], :src [1 0]}
     {:dst [1 2], :src [0 1]}
     {:dst [0 3], :src [1 2]}
     {:dst [1 4], :src [0 3]}
     {:dst [2 1], :src [1 0]}
     {:dst [1 2], :src [2 1]}
     {:dst [2 3], :src [1 2]}
     {:dst [1 4], :src [2 3]})))

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
  '(({:dst [4 6], :src [5 5]}
     {:dst [5 7], :src [4 6]}
     {:dst [5 8], :src [5 7]}
     {:dst [5 9], :src [5 8]}
     {:dst [6 10], :src [5 9]}
     {:dst [6 5], :src [5 5]}
     {:dst [7 6], :src [6 5]}
     {:dst [8 7], :src [7 6]}
     {:dst [9 8], :src [8 7]}
     {:dst [8 9], :src [9 8]}
     {:dst [7 9], :src [8 9]}
     {:dst [6 10], :src [7 9]})))

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
  '(({:dst [6 4], :src [7 3]}
     {:dst [7 5], :src [6 4]}
     {:dst [7 6], :src [7 5]}
     {:dst [6 7], :src [7 6]}
     {:dst [7 8], :src [6 7]}
     {:dst [8 4], :src [7 3]}
     {:dst [7 5], :src [8 4]}
     {:dst [7 6], :src [7 5]}
     {:dst [8 7], :src [7 6]}
     {:dst [7 8], :src [8 7]})))


(deftest same-cell-coord-test-true
 (let [c1 {:x 1 :y 2 :status :wall :surrounded false :player :red}
       c2 {:x 1 :y 2 :status :active :surrounded true :player :blue}]
  (is (true? (wall/same-cell-coord? c1 c2)))))

(deftest same-cell-coord-test-false
  (let [c1 {:x 1 :y 2 :status :wall :surrounded false :player :red}
        c2 {:x 0 :y 2 :status :active :surrounded true :player :blue}]
    (is (false? (wall/same-cell-coord? c1 c2)))))

(defspec same-cell-coord-should-return-boolean
 100
 (prop/for-all [c1 (s/gen :specs/cell) c2 (s/gen :specs/cell)]
               (is (boolean? (wall/same-cell-coord? c1 c2)))))

; (s/fdef wall/same-cell-coord?
;  :args (s/cat :c1 :specs/cell :c2 :specs/cell)
;  :ret boolean?)
;
; (stest/summarize-results (stest/check `wall/same-cell-coord?))


(deftest nearest-cell-cells-true
 (let [c1 {:x 1 :y 1 :status :wall :surrounded false :player :red}
       c2 {:x 0 :y 0 :status :active :surrounded true :player :blue}]
  (is (true? (wall/nearest-cell? c1 c2)))))

(deftest nearest-cell-cells-false
 (let [c1 {:x 1 :y 2 :status :wall :surrounded false :player :red}
       c2 {:x 3 :y 2 :status :active :surrounded true :player :blue}]
  (is (false? (wall/nearest-cell? c1 c2)))))

(deftest wall-of-zero-walls-found
 (is (empty? (wall/walls-of diamond :blue))))

(deftest wall-of-many-walls-found
 (is (= 4 (count (wall/walls-of diamond :red)))))

(deftest left-most-ind-no-tie
 (let [coll [{:y 1, :surrounded false, :status :active, :player :red, :x 6}
             {:y 113, :surrounded true, :status :active, :player :red, :x 44}
             {:y 40, :surrounded false, :status :wall, :player :blue, :x 3}]]
   (is (= 2 (wall/left-most-ind coll)))))

(deftest left-most-ind-tie
 (let [coll [{:y 1, :surrounded false, :status :active, :player :red, :x 3}
             {:y 113, :surrounded true, :status :active, :player :red, :x 44}
             {:y 40, :surrounded false, :status :wall, :player :blue, :x 3}]]
   (is (= 0 (wall/left-most-ind coll)))))

(deftest orientation-checks-horizontal
 (let [a {:y 1, :surrounded false, :status :active, :player :red, :x 2}
       b {:y 1, :surrounded false, :status :active, :player :red, :x 3}
       p-left {:y 2, :surrounded false, :status :active, :player :red, :x 3}
       p-right {:y 0, :surrounded false, :status :active, :player :red, :x 3}
       p-same {:y 1, :surrounded false, :status :active, :player :red, :x 1}]
   (is (= -1 (wall/orientation a b p-left)))
   (is (= 1 (wall/orientation a b p-right)))
   (is (= 0 (wall/orientation a b p-same)))))

(deftest orientation-checks-vertical
 (let [a {:y 1, :surrounded false, :status :active, :player :red, :x 2}
       b {:y 2, :surrounded false, :status :active, :player :red, :x 2}
       p-left {:y 3, :surrounded false, :status :active, :player :red, :x 1}
       p-right {:y 2, :surrounded false, :status :active, :player :red, :x 3}
       p-same {:y 1, :surrounded false, :status :active, :player :red, :x 2}]
   (is (= -1 (wall/orientation a b p-left)))
   (is (= 1 (wall/orientation a b p-right)))
   (is (= 0 (wall/orientation a b p-same)))))

(deftest all-on-right-side-true
 (let [coll [{:y 0, :surrounded false, :status :active, :player :red, :x 3}
             {:y 1, :surrounded true, :status :active, :player :red, :x 44}
             {:y 0, :surrounded false, :status :wall, :player :blue, :x 3}]
        a {:y 1, :surrounded false, :status :active, :player :red, :x 2}
        b {:y 1, :surrounded false, :status :active, :player :red, :x 3}]
   (is (true? (wall/all-on-right-side? a b coll)))))

(deftest all-on-right-side-false
 (let [coll [{:y 5, :surrounded false, :status :active, :player :red, :x 3}
             {:y 113, :surrounded true, :status :active, :player :red, :x 2}
             {:y 40, :surrounded false, :status :wall, :player :blue, :x 1}]
        a {:y 3, :surrounded false, :status :active, :player :red, :x 2}
        b {:y 4, :surrounded false, :status :active, :player :red, :x 2}]
   (is (false? (wall/all-on-right-side? a b coll)))))

(deftest all-on-left-side-true
 (let [coll [{:y 2, :surrounded false, :status :active, :player :red, :x 3}
             {:y 1, :surrounded true, :status :active, :player :red, :x 44}
             {:y 3, :surrounded false, :status :wall, :player :blue, :x 3}]
        a {:y 1, :surrounded false, :status :active, :player :red, :x 2}
        b {:y 1, :surrounded false, :status :active, :player :red, :x 3}]
   (is (true? (wall/all-on-left-side? a b coll)))))

(deftest all-on-left-side-false
 (let [coll [{:y 5, :surrounded false, :status :active, :player :red, :x 3}
             {:y 113, :surrounded true, :status :active, :player :red, :x 2}
             {:y 40, :surrounded false, :status :wall, :player :blue, :x 1}]
        a {:y 3, :surrounded false, :status :active, :player :red, :x 2}
        b {:y 4, :surrounded false, :status :active, :player :red, :x 2}]
   (is (false? (wall/all-on-left-side? a b coll)))))

(deftest active-range-check
 (is (= [4 5] (wall/active-range [0 1 2 3 4 5] 4)))
 (is (= [0 1 2 3 4 5] (wall/active-range [0 1 2 3 4 5] 0)))
 (is (= [5] (wall/active-range [0 1 2 3 4 5] 5))))

(deftest candidates-check
 (let [v [{:y 1, :surrounded false, :status :active, :player :red, :x 3}
          {:y 2, :surrounded true, :status :active, :player :red, :x 2}
          {:y 2, :surrounded false, :status :wall, :player :blue, :x 1}
          {:y 3, :surrounded false, :status :wall, :player :blue, :x 1}
          {:y 3, :surrounded false, :status :wall, :player :blue, :x 2}]]
   (is (= 2 (count (wall/candidates v 2))))
   (is (= '(3) (wall/candidates v 4)))))

(deftest scan-zone-check
 (let [v [{:y 1, :surrounded false, :status :active, :player :red, :x 3}
          {:y 2, :surrounded true, :status :active, :player :red, :x 2}
          {:y 2, :surrounded false, :status :wall, :player :blue, :x 1}
          {:y 3, :surrounded false, :status :wall, :player :blue, :x 1}
          {:y 3, :surrounded false, :status :wall, :player :blue, :x 2}]
       zone (wall/scan-zone v 2)]
   (is (= (get-in v [2 :y]) (get-in zone [0 :y])))
   (is (= 3 (count zone)))))

(deftest diamond-shape-wall
 (is (= diamond-wall (wall/get-walls diamond :red))))

(deftest square-shape-wall
 (is (= square-wall (wall/get-walls square :red))))

(deftest vertical-eight-shape-wall
  (is (= vertical-eight-wall (wall/get-walls vertical-eight :red))))

(deftest js-error-out-of-bounds-walls-check
 (is (= js-error-out-of-bounds-walls (wall/get-walls js-error-out-of-bounds :red))))

(deftest js-error-out-of-bounds-2-walls-check
 (is (= js-error-out-of-bounds-2-walls (wall/get-walls js-error-out-of-bounds-2 :red))))
