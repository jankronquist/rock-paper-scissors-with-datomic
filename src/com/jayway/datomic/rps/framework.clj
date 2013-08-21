(ns com.jayway.datomic.rps.framework
  (:require [datomic.api :refer [db] :as d]
            [com.jayway.datomic.rps.core :as c]))

(defn initialize-schema [conn]
  (let [schema-tx (read-string (slurp "resources/schema.dtm"))]
    @(d/transact conn schema-tx)))

(defn create-entity [conn]
  "Returns the id of the new entity."
  (let [temp-id (d/tempid :db.part/user)
        optimistic-concurrency [:db.fn/cas temp-id :entity/version nil 0]
        tx @(d/transact conn [{:db/id temp-id} optimistic-concurrency])]
    (d/resolve-tempid (db conn) (:tempids tx) temp-id)))

(defn handle-command [{:keys [aggregate-id] :as command} conn]
  "Apply the command to its target aggregate using optimistic concurrency. Returns the datomic transaction."
  (let [state (d/entity (db conn) aggregate-id)
        modification (c/perform command state)
        old-version (:entity/version state)
        next-version ((fnil inc -1) old-version)
        optimistic-concurrency [:db.fn/cas aggregate-id :entity/version old-version next-version]
        tx @(d/transact conn (conj modification optimistic-concurrency))]
    tx))
