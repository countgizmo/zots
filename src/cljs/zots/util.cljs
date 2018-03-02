(ns cljs.zots.util
  (:require [clojure.string :as string]
            [goog.net.cookies :as cookies]))

(defn coord->screen [n] (* 30 (inc n)))

(defn screen->coord [n] (dec (/ n 30)))

(defn get-url
 []
 (-> (.-location js/window) (string/split #"\?") last))

(defn get-game-id-from-url
 []
 (-> (get-url) (string/split #"/") last))

(defn get-player-cookie
 []
 (-> (.get goog.net.cookies "player") keyword))

(defn set-player-cookie
  [player]
  (.set goog.net.cookies
    "player"
    (name player)
    (* 60 60 24)
    (str "/game/" (get-game-id-from-url))))
