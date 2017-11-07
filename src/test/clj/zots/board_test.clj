(ns test.clj.zots.board-test
 (:require [clojure.test :refer :all])
 (:require [clj.zots.board :as board]))

(def simple-surround
 [[{:y 0, :surrounded false, :status :active, :player :none, :x 0}
   {:y 0, :surrounded false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 1, :surrounded false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded false, :status :active, :player :blue, :x 1}
   {:y 1, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 2, :surrounded false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded false, :status :active, :player :red, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 2}]])

(defn make-state [b t]
  {:board b
   :target t
   :visited []
   :trail []})

(deftest surround-state-detection
 (testing "A simple situation when a cell is surrounded by enemy"
   (let [next-state (board/next-state (make-state simple-surround [1 1]))]
     (is (true? (get-in next-state [:board 1 1 :surrounded]))))))
