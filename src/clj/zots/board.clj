(ns zots.board (:require [proto-repl.saved-values]))

(def min-x 0)
(def min-y 0)
(def max-x 2)
(def max-y 2)

(defn visited?
 [x y v]
 (true? (some #(and (= (:x %) x) (= (:y %) y)) v)))

(defn add-visited
 [x y v]
 (if (visited? x y v) v (conj v {:x x :y y})))

(defn should-visit?
 "Checks if the coordinates are not outside our world"
 [x y]
 (not (or
        (< x min-x)
        (< y min-y)
        (> x max-x)
        (> y max-y))))

(defn flood-cell?
 [x y visited]
 (and
   (not (visited? x y visited))
   (should-visit? x y)))

(defn check-cell
 [[x y] state]
 (if (flood-cell? x y (:visited state))
   (fill-flood x y state)
   state))

(defn row->str [r]
 (reduce #(str %1 (-> %2 :player player->symbol)) "" r))

(defn board->row [b]
 (reduce #(str %1 (row->str %2) "\n") "" b))

(defn print-board [state]
 (print (board->row (get-in state [:board]))))

(print-board state)



; todo:
; 1. Write flooding algorith only - use invisible ink :P
; 2. Parse flooded state.
; 3. Paint walls, change statuses accordingly to flooded state.

(defn fill-flood
 [x y state]
 (let [new-visited (add-visited x y (:visited state))
       new-state (parse-cell x y state)
       north-coord [x (inc y)]
       north-east-coord [(inc x) (inc y)]
       east-coord [(inc x) y]
       south-east-coord [(inc x) (dec y)]
       south-coord [x (dec y)]
       south-west-coord [(dec x) (dec y)]
       west-coord [(dec x) y]
       north-west-coord [(dec x) (dec y)]]
   (->> (assoc-in new-state [:visited] new-visited)
     (check-cell north-coord)
     (check-cell north-east-coord)
     (check-cell east-coord)
     (check-cell south-east-coord)
     (check-cell south-coord)
     (check-cell south-west-coord)
     (check-cell west-coord)
     (check-cell north-west-coord))))

(fill-flood 1 1 {:board simple-surround :visited [] :target [1 1]})

(def test-state
 {:board simple-surround
  :target [1 0]
  :visited []})

(def tri-problem
 [[{:y 0, :surrounded :false, :status :active, :player :red, :x 0}
   {:y 0, :surrounded :false, :status :active, :player :blue, :x 1}
   {:y 0, :surrounded :false, :status :active, :player :red, :x 2}]])

(def simple-surround
 [[{:y 0, :surrounded :false, :status :active, :player :blue, :x 0}
   {:y 0, :surrounded :false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded :false, :status :active, :player :red, :x 2}]
  [{:y 1, :surrounded :false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded :false, :status :active, :player :blue, :x 1}
   {:y 2, :surrounded :false, :status :active, :player :red, :x 1}]
  [{:y 2, :surrounded :false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded :false, :status :active, :player :red, :x 1}
   {:y 2, :surrounded :false, :status :active, :player :red, :x 2}]])

(defn next-state
 [board]
 (fill-flood 0 0 board []))
