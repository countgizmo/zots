(ns clj.zots.db-schema)

(def schema
  [{:db/ident :game/cells
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident :game/id
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value}

   {:db/ident :cell/surrounded?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :coord/x
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :coord/y
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident :cell/player
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Which player owns the cell"}

   {:db/ident :cell/status
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :active}
   {:db/ident :wall}
   {:db/ident :red}
   {:db/ident :blue}
   {:db/ident :none}])
