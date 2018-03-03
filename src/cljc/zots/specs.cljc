(ns cljc.zots.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::x (s/and int? #(>= % 0)))
(s/def ::y (s/and int? #(>= % 0)))
(s/def ::surrounded boolean?)
(s/def ::status #{:wall :active})
(s/def ::player #{:red :blue :none})
(s/def :specs/cell (s/keys :req-un [::x ::y ::surrounded ::status ::player]))

(s/def ::surrounded? boolean?)
(s/def :specs/coord
  (s/coll-of integer? :count 2))
(s/def :specs/zot
  (s/map-of :specs/coord
    (s/keys :req-un [::surrounded? ::status ::player])))

; (s/explain :specs/zot
;   {[0 0] {:surrounded? true :player :red :status :active}})


(s/fdef ::same-cell-coord?
 :args (s/cat :c1 ::cell :c2 ::cell)
 :ret boolean?)

(s/def :specs/board
 (s/coll-of (s/coll-of :specs/cell :count 17) :count 20))

(s/def :specs/turn #{:red :blue})
(s/def :specs/score (s/map-of :specs/turn int?))
(s/def :specs/wall (s/map-of keyword? (s/coll-of int?)))

(s/def :specs/walls
 (s/map-of :specs/turn
  (s/or :list list? :list-of-lists (s/coll-of (s/coll-of :specs/wall)))))

(s/def :specs/slots
  (s/coll-of #{:red :blue :none} :kind set?))

(s/def :specs/game
 (s/keys :req-un
  [:specs/board
   :specs/turn
   :specs/score
   :specs/walls
   :specs/slots]))

(s/def :specs/move
 (s/keys :req-un [::x ::y :specs/turn]))

(def game-example
 {:board [[{:y 0, :surrounded false, :status :active, :player :none, :x 0}
           {:y 0, :surrounded false, :status :wall, :player :red, :x 1}
           {:y 0, :surrounded false, :status :active, :player :none, :x 2}]
          [{:y 1, :surrounded false, :status :wall, :player :red, :x 0}
           {:y 1, :surrounded true, :status :active, :player :blue, :x 1}
           {:y 1, :surrounded false, :status :wall, :player :red, :x 2}]
          [{:y 2, :surrounded false, :status :active, :player :none, :x 0}
           {:y 2, :surrounded false, :status :wall, :player :red, :x 1}
           {:y 2, :surrounded false, :status :active, :player :none, :x 2}]]
  :turn :red
  :score {:red 1 :blue 0}
  :walls {:red '() :blue '(({:src [2 0], :dst [1 1]} {:src [1 1], :dst [2 2]} {:src [2 0], :dst [3 1]} {:src [3 1], :dst [2 2]}))}})

;(s/explain :specs/walls '{:red '() :blue '(({:src [2 0], :dst [1 1]} {:src [1 1], :dst [2 2]} {:src [2 0], :dst [3 1]} {:src [3 1], :dst [2 2]}))})
