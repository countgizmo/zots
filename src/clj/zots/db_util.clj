(ns clj.zots.db-util
  (:require [datomic.client.api :as d]
            [clj.zots.db-schema :refer [schema]]
            [cljc.zots.game :as game]))

(def cfg
  {:server-type :peer-server
   :access-key "myaccesskey"
   :secret "mysecret"
   :endpoint "localhost:8998"})

(def client (d/client cfg))

(defn apply-schema
  [conn]
  (d/transact conn {:tx-data schema}))

(defn init-db
  []
  (let [conn (d/connect client {:db-name "hello"})
        db (d/db conn)]
    (apply-schema conn)
    [conn db]))

(def game-q
  '[:find
     (pull ?e
      [:game/turn
       {:game/slots [:db/ident]}
       {:game/cells
         [:coord/x
          :coord/y
          :cell/player
          :cell/surrounded?
          :cell/status]}])
    :in $ ?id
    :where [?e :game/id ?id]])

(defn get-slots
  [game]
  (into [] (:slots game)))

(defn cell->db
  [[[x y] {:keys [surrounded? player status]}]]
  {:coord/x x
   :coord/y y
   :cell/surrounded? surrounded?
   :cell/player player
   :cell/status status})

(defn get-cells
  [game]
  (let [board (:board game)]
    (mapv cell->db board)))

(defn game->game-data
  [id game]
  {:game/id (Long. id)
   :game/slots (get-slots game)
   :game/turn (:turn game)
   :game/cells (get-cells game)})

(defn db-cells->board
  [cells]
  (into {}
    (map
      (fn [{surrounded? :cell/surrounded?
            x :coord/x
            y :coord/y
            status :cell/status
            player :cell/player}]
        {[x y]
         {:surrounded? surrounded?
          :player player
          :status status}})
      cells)))

(defn game-data->game
  [data]
  (let [{turn :game/turn
         slots :game/slots
         cells :game/cells}
        (ffirst data)]
    {:turn turn
     :slots (into #{} (map :db/ident slots))
     :board (db-cells->board cells)
     :score {:red 0 :blue 0}
     :walls {:red '() :blue '()}}))

(defn find-game-by-id
  [conn game-id]
  (let [db (d/db conn)
        id (Long. game-id)]
    (-> (d/q game-q db id))))

(defn find-game
  [conn game-id]
  (-> (find-game-by-id conn game-id)
      (game-data->game)
      (game/enrich-from-db)))

(defn game-exists?
  [conn game-id]
  (not (empty? (find-game-by-id conn game-id))))

(defn find-board
  [conn game-id]
  (-> (find-game-by-id conn game-id)
      ffirst
      (get :game/cells {})
      (db-cells->board)))

(defn find-cells
  [db game-id coord-coll]
  (let [id (Long. game-id)]
    (d/q '[:find ?cells
           :in  $ ?id [[?x ?y]]
           :where [?e :game/id ?id]
                  [?e :game/cells ?cells]
                  [?cells :coord/x ?x]
                  [?cells :coord/y ?y]]
          db id coord-coll)))

(defn generate-retractions-for-cells
  [cell-ids]
  (mapv #(vec [:db.fn/retractEntity %]) cell-ids))

(defn get-ids-for-cells-to-update
  [db game-id cell-coords]
  (if (nil? cell-coords)
    []
    (-> (find-cells db game-id cell-coords) flatten)))


(defn update-game-state
  [conn game-id {:keys [board turn slots]}]
  (let [db (d/db conn)
        game-id (Long. game-id)
        old-board (find-board conn game-id)
        cells (game/updated-cells-between-boards old-board board)
        cell-coords (or (keys cells) [])
        cids (-> (find-cells db game-id cell-coords) flatten)
        retractions (generate-retractions-for-cells cids)
        assertions (when cids (mapv cell->db cells))]
    (d/transact conn
     {:tx-data
       (conj retractions
         (merge
           {:game/id game-id
            :game/turn turn
            :game/slots slots}
           (when cids {:game/cells assertions})))})))

(defn save-game
  [conn id game]
  (d/transact conn
    {:tx-data [(game->game-data id game)]}))

(defn update-game
  [conn id game]
  (if (game-exists? conn id)
    (update-game-state conn id game)
    (save-game conn id game)))
