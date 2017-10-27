(ns zots.board)

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

(defn fill-flood
 [x y board visited]
 (println (str "Visiting " x " " y))
 (let [v (add-visited x y visited)]
   (if (and (not (visited? (dec x) y v)) (should-visit? (dec x) y))
     (fill-flood (dec x) y board v)
     board)
   (if (and (not (visited? (dec x) (dec y) v)) (should-visit? (dec x) (dec y)))
     (fill-flood (dec x) (dec y) board v)
     board)
   (if (and (not (visited? (inc x) y v)) (should-visit? (inc x) y))
     (fill-flood (inc x) y board v)
     board)
   (if (and (not (visited? (inc x) (inc y) v)) (should-visit? (inc x) (inc y)))
     (fill-flood (inc x) (inc y) board v)
     board)))

(def simple-surround
 [[{:y 0, :surrounded :false, :status :active, :player :red, :x 0}
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
