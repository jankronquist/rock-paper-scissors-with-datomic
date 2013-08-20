(ns com.jayway.datomic.rps.framework
  (:require [datomic.api :as d]
            [com.jayway.datomic.rps.core :as c]))

(defn initialize-schema [conn]
  (let [schema-tx (read-string (slurp "resources/schema.dtm"))]
    @(d/transact conn schema-tx)))

(defn create-entity [conn]
  "Returns the id of the new entity."
  (let [optimistic-concurrency [:db.fn/cas #db/id[:db.part/user -1] :entity/version nil 0]
        tx @(d/transact conn [{:db/id #db/id[:db.part/user -1]}
                              optimistic-concurrency])]
    (first (vals (:tempids tx)))))

(defn handle-command [command conn]
  "Apply the command to its target aggregate using optimistic concurrency. Returns the datomic transaction."
  (let [aggregate-id (:aggregate-id command)
        state (-> conn d/db (d/entity aggregate-id))
        modification (c/perform command state)
        old-version (:entity/version state)
        next-version (if (nil? old-version) 0 (inc old-version))
        optimistic-concurrency [:db.fn/cas aggregate-id :entity/version old-version next-version]
        tx @(d/transact conn (conj modification optimistic-concurrency))]
    tx))
