(ns cljc.zots.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::x (s/and int? #(>= % 0)))
(s/def ::y (s/and int? #(>= % 0)))
(s/def ::surrounded boolean?)
(s/def ::status #{:wall :active})
(s/def ::player #{:red :blue})
(s/def :specs/cell (s/keys :req-un [::x ::y ::surrounded ::status ::player]))

(s/fdef ::same-cell-coord?
 :args (s/cat :c1 ::cell :c2 ::cell)
 :ret boolean?)
