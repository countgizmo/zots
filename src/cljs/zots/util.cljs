(ns cljs.zots.util
  (:require [clojure.string :as string]))

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
 (let [id (get-game-id-from-url)
       ck (str "player_" id)]
   (-> (.get goog.net.cookies ck) keyword)))
