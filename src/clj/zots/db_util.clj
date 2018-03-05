(ns clj.zots.db-util
  (:require [datomic.client.api :as d]
            [clj.zots.db-schema :refer [schema]]))

(def cfg
  {:server-type :peer-server
   :access-key "myaccesskey"
   :secret "mysecret"
   :endpoint "localhost:8998"})

(defonce client (d/client cfg))

(defn apply-schema
  [conn]
  (d/transact conn {:tx-data schema}))

(defn init-db
  []
  (let [conn (d/connect client {:db-name "hello"})
        db (d/db conn)]
    (apply-schema conn)
    [conn db]))

; (def add-cells
;   {:game/id 1
;    :game/cells
;     [{:coord/x 1 :coord/y 1 :cell/surrounded? true :cell/player :red :cell/status :active}
;      {:coord/x 0 :coord/y 0 :cell/surrounded? false :cell/player :blue :cell/status :wall}
;      {:coord/x 2 :coord/y 2 :cell/surrounded? false :cell/player :none :cell/status :active}]})
;

(defn get-slots
  [game]
  (into [] (:slots game)))

(defn get-cells
  [game]
  (let [board (-> (:board game) (flatten))]
    (mapv
      (fn [{:keys [x y surrounded player status]}]
        {:coord/x x
         :coord/y y
         :cell/surrounded? surrounded
         :cell/player player
         :cell/status status})
      board)))

(defn game->game-data
  [id game]
  {:game/id (Long. id)
   :game/slots (get-slots game)
   :game/turn (:turn game)
   :game/cells (get-cells game)})

(defn save-game
  [conn id game]
  (let [data (game->game-data id game)]
    (d/transact conn {:tx-data [data]})))

(def game-by-id-q
  '[:find ?x ?y ?player ?status ?surrounded
        :where [?e :game/id ?id]
               [?e :game/cells ?cell]
               [?cell :cell/player ?pl-ref]
               [?pl-ref _ ?player]
               [?cell :coord/x ?x]
               [?cell :coord/y ?y]
               [?cell :cell/surrounded? ?surrounded]
               [?cell :cell/status ?st-ref]
               [?st-ref _ ?status]])

(defn game-data->game
  [data])


(defn find-game
  [conn id]
  (->> (d/db conn)
       (d/q game-by-id-q)
       (game-data->game)))
