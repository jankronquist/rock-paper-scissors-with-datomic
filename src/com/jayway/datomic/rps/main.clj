(ns com.jayway.datomic.rps.main
  (:require [datomic.api :as datomic] 
            [com.jayway.datomic.rps.core :as c]
            [com.jayway.datomic.rps.framework :as f] 
            [com.jayway.datomic.rps.domain :as domain]))

(def uri "datomic:mem://game")
(datomic/create-database uri)
(def conn (datomic/connect uri))

(defn print-entity [entity-id]
  (let [e (-> conn datomic/db (datomic/entity entity-id))]
	  (println "entity: " (datomic/touch e))))

(f/initialize-schema conn)

(def ply1 (f/create-entity conn "player"))
(def ply2 (f/create-entity conn "player"))
(def game-id (f/create-entity conn "game"))

(defn -main [& args]
  (f/handle-command (domain/->SetPlayerEmailCommand ply1 "one@example.com") conn)
  (f/handle-command (domain/->SetPlayerEmailCommand ply2 "two@example.com") conn)
  (f/handle-command (domain/->CreateGameCommand game-id ply1 :move.type/rock) conn)
  (f/handle-command (domain/->DecideMoveCommand game-id ply2 :move.type/scissors) conn)
  (print-entity game-id)
  (datomic/shutdown true))
