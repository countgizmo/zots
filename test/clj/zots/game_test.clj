(ns clj.zots.game-test
 (:require [clojure.test :refer :all]
           [cljc.zots.game :as game]
           [cljc.zots.specs :as specs]
           [clojure.spec.alpha :as s]
           [clojure.test.check.clojure-test :refer [defspec]]
           [clojure.test.check.generators :as gen]
           [clojure.test.check.properties :as prop]))

(deftest valid-turn-check
  (let [game-state (-> (gen/generate (s/gen :specs/game))
                      (assoc :turn :red))]
   (is (game/valid-turn? game-state :red))
   (is (not (game/valid-turn? game-state :blue)))))

(deftest cell-available-check
  (let [cell (-> (gen/generate (s/gen :specs/cell))
                 (assoc :status :active :player :none :surrounded false))]
    (is (game/cell-available? cell))))

(deftest cell-occupied-check
  (let [cell (-> (gen/generate (s/gen :specs/cell))
                 (assoc :status :active :player :blue :surrounded false))]
    (is (not (game/cell-available? cell)))))

(deftest cell-surrounded-check
  (let [cell (-> (gen/generate (s/gen :specs/cell))
                 (assoc :status :active :player :red :surrounded true))]
    (is (not (game/cell-available? cell)))))

(deftest cell-wall-check
  (let [cell (-> (gen/generate (s/gen :specs/cell))
                 (assoc :status :wall :player :red :surrounded false))]
    (is (not (game/cell-available? cell)))))

(deftest random-turn-check
  (is (s/valid? :specs/turn (game/random-turn))))

(deftest generate-valid-game
 (is (s/valid? :specs/game (game/new-game))))

(defspec you-are-not-your-enemy
 100
 (prop/for-all [me (s/gen :specs/turn)]
               (is (not= (game/enemy me) me))))

(deftest red-is-enemy-for-blue
 (is (= :red (game/enemy :blue))))

(deftest cell-taken-by-red-is-true
 (let [cell (-> (gen/generate (s/gen :specs/cell))
                (assoc :player :blue :surrounded true))]
   (is (game/cell-taken-by? :red cell))))

(deftest red-cell-taken-by-red-is-false
 (let [cell (-> (gen/generate (s/gen :specs/cell))
                (assoc :player :red :surrounded true))]
   (is (not (game/cell-taken-by? :red cell)))))

(deftest empty-cell-taken-by-red-is-false
 (let [cell (-> (gen/generate (s/gen :specs/cell))
                (assoc :player :none :surrounded true))]
   (is (not (game/cell-taken-by? :red cell)))))

(deftest free-cell-taken-by-red-is-false
 (let [cell (-> (gen/generate (s/gen :specs/cell))
                (assoc :player :blue :surrounded false))]
   (is (not (game/cell-taken-by? :red cell)))))

(def game-state
  (-> (game/new-game)
      (assoc-in [:board 0 1 :player] :red)
      (assoc-in [:board 1 0 :player] :red)
      (assoc-in [:board 1 1 :player] :blue)
      (assoc-in [:board 1 1 :surrounded] true)
      (assoc-in [:board 1 2 :player] :red)
      (assoc-in [:board 2 1 :player] :red)))

(deftest score-calculation-check
  (let [blue-score (game/calculate-score game-state :blue)
        red-score (game/calculate-score game-state :red)]
    (is (zero? blue-score))
    (is (= 1 red-score))))

(deftest valid-move-checks
 (let [g (game/new-game)
       turn (:turn g)]
   (is (game/valid-move? g {:turn turn :x 0 :y 0}))
   (is (not (game/valid-move? g {:turn (game/enemy turn) :x 0 :y 0})))
   (is (game/valid-move? g {:turn turn :x 10 :y 5}))
   (is (not (game/valid-move? g {:turn turn :x -1 :y 0})))))
