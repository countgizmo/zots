(ns cljs.zots.util
  (:require [clojure.string :as string]
            [goog.net.cookies :as cookies]
            [goog.dom :as gdom]))

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

(defn clear-tab-notification
  []
  (set! (. (gdom/getDocument) -title) "Zots"))

(defn set-tab-notification
  [turn]
  (let [pl (get-player-cookie)]
    (if (and
          (= "hidden" (. (gdom/getDocument) -visibilityState))
          (= pl turn))
      (set! (. (gdom/getDocument) -title) "* Zots")
      (clear-tab-notification))))
