(ns clj.zots.db-util
  (:require [datomic.client.api :as d]
            [clj.zots.db-schema :refer [schema]]
            [cljc.zots.game :as game]))

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

; (let [[conn db] (init-db)]
;   (-> (find-game conn 250279719244)))

;   ;(save-game conn 13 (game/new-game)))
;   ;(d/q game-q db 10))
;
; (let [[conn db] (init-db)]
;    (save-game conn 187814819201 (game/new-game)))
; ;
; (let [[conn db] (init-db) game-id 187814819201
;       g (find-game conn game-id)
;       new-game (game/make-move g {:x 14 :y 15 :turn :blue})]
;     (time (update-game conn game-id new-game)))
; ;
; (let [[conn db] (init-db)]
;   (d/q '[:find (pull ?cells [*])
;          :in $ [[?x ?y]]
;          :where [?e :game/id 525136119248]
;                 [?e :game/cells ?cells]
;                 [?cells :coord/x ?x]
;                 [?cells :coord/y ?y]]
;         db [[9 2]]))
;
; (let [[conn db] (init-db)]
;   (d/q '[:find (pull ?cells [*])
;          :in $
;          :where [?e :game/id 525136119248]
;                 [?e :game/cells ?cells]]
;         db))
;
; ; ; ; ;
; ; ;
; (def game
;   (let [[conn db] (init-db)]
;     (find-game conn 187814819201)))
; ;
; (let [[conn db] (init-db)]
;   (d/q '[:find ?turn ?cells
;          :where [?e :game/id 187814819201]
;                 [?e :game/turn ?turn]
;                 [?e :game/cells ?cells]]
;         db))

; (let [[conn db] (init-db)]
;   (d/transact conn
;     {:tx-data
;       [{:db/ident :upsert-cell
;         :db/fn #db/fn
;         {:lang :clojure
;          :params [db gid m]
;          :code (if-let [id (get-cell-id db gid m)]
;                 [(dissoc m :coord/x :coord/y)]
;                 [m])}}]}))

;
; (def hello
;   #db/fn {:lang :clojure
;           :params [name]
;           :code (str "Hello, " name)})
;
; (def do-shit
;   #db/fn {:lang :clojure
;           :params [db gid m]
;           :code (if-let [id (get-cell-id db gid m)]
;                  [(-> (dissoc m :coord/x :coord/y)
;                      (assoc :db/id id))]
;                  [m])})
; ;
; (let [[conn db] (init-db)]
;   (d/transact
;     conn
;     {:tx-data
;       [[:db/fn do-shit 187814819201]]}))

;
; (let [[conn db] (init-db)]
;   (d/transact conn
;     {:tx-data
;       [[:db.fn/retractEntity 1759218606308]]}))
;   ;(save-game conn 187814819201 (game/make-move game {:x 14 :y 15 :turn :red})))
;
; (let [[conn db] (init-db)]
;   (d/q '[:find (pull ?cells [*])
;          :in $ [[?x ?y]]
;          :where [?e :game/id 965526419222]
;                 [?e :game/cells ?cells]
;                 [?cells :coord/x ?x]
;                 [?cells :coord/y ?y]]
;         db [[7 3] [8 3]]))
;
; (let [[conn db] (init-db)]
;   (-> (find-game conn 965526419222)
;       :board
;       (get [8 2])))

; (let [[conn db] (init-db)]
;   (find-cell conn 187814819201 10 10))
;
; (let [[conn db] (init-db)]
;   (d/transact conn
;     {:tx-data
;       [[:db.fn/retractEntity 17592186045774]]}))
;
; (def cell
;   [{:cell/surrounded? true
;     :coord/x 10
;     :coord/y 10
;     :cell/status :active
;     :cell/player :red}])


; (let [[conn db] (init-db)]
;   (d/transact conn
;     {:tx-data
;       [[:db.fn/retractEntity 17592186045783]
;        [:db.fn/retractEntity 17592186045790]
;        {:game/id 187814819201
;         :game/turn :red
;         :game/cells
;         [{:coord/x 10 :coord/y 10 :cell/surrounded? true :cell/player :red :cell/status :active}]}]}))


; ;
; (let [[conn db] (init-db)]
;   (d/transact conn
;     {:tx-data
;       [{:game/id 1
;         :game/slots [:red :blue]
;         :game/cells (get-cells (game/new-game))}]}))
; ;         ;[:coord/x 8, :coord/y 8, :cell/surrounded? false, :cell/player :none, :cell/status :active]]]}))
; ; ;
; ; ;
;
;

; (def cells
;   (let [[conn db] (init-db)]
;     (d/q '[:find (pull ?cells [*])
;            :where [?e :game/id 187814819201]
;                   [?e :game/cells ?cells]]
;       db)))
;
;
; (def datom
;   (let [[conn db] (init-db)]
;       (map #(map :v (d/datoms db :eavt (:e %) :game/cells)
;                (d/datoms db :avet :game/id 1)))))
