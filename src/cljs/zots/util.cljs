(ns cljs.zots.util)

(defn coord->screen [n] (* 30 (inc n)))

(defn screen->coord [n] (dec (/ n 30)))
