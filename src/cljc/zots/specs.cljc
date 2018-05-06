(ns cljc.zots.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::surrounded? boolean?)
(s/def ::status #{:wall :active})
(s/def ::player #{:red :blue :none})
(s/def :specs/cell (s/keys :req-un [::surrounded? ::status ::player]))

(s/def :specs/coord
  (s/coll-of integer? :count 2))

(s/def :specs/zot
  (s/map-of :specs/coord :specs/cell))

(s/fdef ::same-cell-coord?
 :args (s/cat :c1 ::cell :c2 ::cell)
 :ret boolean?)

(s/def :specs/board
 (s/map-of :specs/coord
   (s/keys :req-un [::surrounded? ::status ::player])))

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
