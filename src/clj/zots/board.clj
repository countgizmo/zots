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
 (let [cell (get-in state [:board y x])
       [tx ty] (:target state)
       target-color (get-in state [:board ty tx :player])]
   (and
     (can-fill? cell target-color)
     (not (visited? x y visited))
     (should-visit? x y))))

(defn check-cell
 [[x y] state]
 (if (flood-cell? x y (:visited state))
   (fill-flood x y state)
   state))

(def player->symbol
 {:red "X" :blue "O" :blue-fill "+" :none "_"})

(defn row->str [r]
 (reduce #(str %1 (-> %2 :player player->symbol)) "" r))

(defn board->row [b]
 (reduce #(str %1 (row->str %2) "\n") "" b))

(defn print-board [state]
 (print (board->row (get-in state [:board]))))

(defn can-fill?
 [cell target-color]
 (or
   (= (:player cell) target-color)
   (= (:player cell) :none)))

(defn parse-cell
 [x y state]
 (let [cell (get-in state [:board y x])
       [tx ty] (:target state)
       target-color (get-in state [:board ty tx :player])
       filled-cell (fill-cell cell target-color)]
  (assoc-in state [:board y x] filled-cell)))

(defn touch-border?
 [[x y] board]
 (or
  (zero? x) (zero? y)
  (= y (count board))
  (= x (count (first board)))))

(defn update-trail
 ([x y state]
  (let [cell (get-in state [:board y x])
        [tx ty] (:target state)
        target-color (get-in state [:board ty tx :player])]
   (if (can-fill? cell target-color)
     (update-in state [:trail] conj [x y])
     state))))

; todo:
; 2. Parse flooded state.
; 3. Paint walls, change statuses accordingly to flooded state.

(defn fill-flood
 [x y state]
 (let [new-visited (add-visited x y (:visited state))
       new-state (update-trail x y state)]
   (->> (assoc-in new-state [:visited] new-visited)
     (check-cell [x (inc y)])
     (check-cell [(inc x) y])
     (check-cell [x (dec y)])
     (check-cell [(dec x) y]))))

(defn mark-surrounded
 [x y state]
 (let [touches (map #(touch-border? % (:board state)) (:trail state))]
  (if (not-every? false? touches)
    state
    (assoc-in state [:board y x :surrounded] true))))

(def filled (fill-flood 1 1 test-state))
(mark-surrounded 1 1 filled)

(print-board (fill-flood 1 1 test-state))



(def test-state
 {:board simple-surround
  :target [1 1]
  :visited []
  :trail []})

(def simple-surround
 [[{:y 0, :surrounded false, :status :active, :player :none, :x 0}
   {:y 0, :surrounded false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 1, :surrounded false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded false, :status :active, :player :blue, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 1}]
  [{:y 2, :surrounded false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded false, :status :active, :player :red, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 2}]])

(defn next-state
 [board]
 (fill-flood 0 0 board []))
