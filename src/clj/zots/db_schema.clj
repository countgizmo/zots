(ns clj.zots.db-schema
  (:require [datomic.client.api :as d]))

(defn get-cell-id
  [db game-id x y]
  (ffirst
    (d/q '[:find ?e
           :in $ ?gid ?x ?y
           :where [?e :game/id ?gid]
                  [?e :game/cells ?cells]
                  [?cells :coord/x ?x]
                  [?cells :coord/y ?y]]
      db game-id x y)))

(def schema
  [{:db/ident :game/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity}
   {:db/ident :game/turn
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Whose turn it is next"}
   {:db/ident :game/slots
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}
   {:db/ident :game/walls
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}
   {:db/ident :game/cells
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}
   {:db/ident :cell/surrounded?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}
   {:db/ident :coord/x
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/index true}
   {:db/ident :coord/y
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/index true}
   {:db/ident :cell/player
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Which player owns the cell"}
   {:db/ident :cell/status
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident :active}
   {:db/ident :wall}
   {:db/ident :turn/red}
   {:db/ident :turn/blue}
   {:db/ident :red}
   {:db/ident :blue}
   {:db/ident :none}])
