(ns com.jayway.datomic.rps.domain
  (:require [com.jayway.datomic.rps.core :as c]))

(defrecord SetPlayerEmailCommand [aggregate-id email])

(defrecord CreateGameCommand [aggregate-id player move])
(defrecord DecideMoveCommand [aggregate-id player move])

(defmulti compare-moves vector)
(defmethod compare-moves [:move.type/rock :move.type/rock] [x y] :tie)
(defmethod compare-moves [:move.type/rock :move.type/paper] [x y] :loss)
(defmethod compare-moves [:move.type/rock :move.type/scissors] [x y] :victory)
(defmethod compare-moves [:move.type/paper :move.type/rock] [x y] :victory)
(defmethod compare-moves [:move.type/paper :move.type/paper] [x y] :tie)
(defmethod compare-moves [:move.type/paper :move.type/scissors] [x y] :loss)
(defmethod compare-moves [:move.type/scissors :move.type/rock] [x y] :loss)
(defmethod compare-moves [:move.type/scissors :move.type/paper] [x y] :victory)
(defmethod compare-moves [:move.type/scissors :move.type/scissors] [x y] :tie)

(extend-protocol c/CommandHandler
  
  SetPlayerEmailCommand
  (c/perform [command state]
    [{:db/id (:aggregate-id command)
      :player/email (:email command)}])
  
  CreateGameCommand
  (c/perform [command state]
    (when (:game/state state)
      (throw (Exception. "Already in started")))
    [{:db/id #db/id[:db.part/user -1]
      :move/player (:player command)
      :move/type (:move command)}
     {:db/id (:aggregate-id command)
      :game/moves #db/id[:db.part/user -1]
      :game/state :game.state/started
      :game/created-by (:player command)}])

  DecideMoveCommand
  (c/perform [command state]
    (when-not (= (:game/state state) :game.state/started)
      (throw (Exception. "Incorrect state")))
    (when (= (:db/id (:game/created-by state)) (:player command))
      (throw (Exception. "Cannot play against yourself")))
    (let [creator-move (:move/type (first (:game/moves state)))
          creator-id (:db/id (:game/created-by state))]
	    [{:db/id #db/id[:db.part/user -1]
	      :move/player (:player command)
	      :move/type (:move command)}
	     (merge {:db/id (:aggregate-id command)
              :game/moves #db/id[:db.part/user -1]}
	            (case (compare-moves (:move command) creator-move)
	              :victory {:game/state :game.state/won 
	                        :game/winner (:player command) 
	                        :game/loser creator-id}   
	              :loss {:game/state :game.state/won 
	                        :game/winner creator-id 
	                        :game/loser (:player command)}
	              :tie {:game/state :game.state/tied}))])))
