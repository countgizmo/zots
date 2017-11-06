(ns zots.board-test
 (:require [clojure.test :refer :all])
 (:require [zots.board :as board]))

(defn generate-active
 [x y pl]
 {:x x :y y :player pl :surrounded :false :status :active})

(defn generate-active-red
 [x y]
 (generate-active x y :red))

(defn generate-active-blue
 [x y]
 (generate-active x y :blue))

(def simple-surround
 [[(generate-active-red 0 0) (generate-active-red 1 0) (generate-active-red 2 0)]
  [(generate-active-red 0 1) (generate-active-blue 1 1) (generate-active-red 1 2)]
  [(generate-active-red 0 2) (generate-active-red 1 2) (generate-active-red 2 2)]])

(deftest surround-state-detection
 (testing "A simple situation when a cell is surrounded by enemy"
   (let [next-state (board/next-state simple-surround)]
     (is (true? (get-in next-state [:board 1 1 :surrounded]))))))
