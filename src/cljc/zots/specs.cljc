(ns cljc.zots.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::x number?)
(s/def ::y number?)
(s/def ::surrounded boolean?)
(s/def ::status #{:wall :active})
(s/def ::player #{:red :blue})
(s/def ::cell (s/keys :req-un [::x ::y ::surrounded ::status ::player]))
