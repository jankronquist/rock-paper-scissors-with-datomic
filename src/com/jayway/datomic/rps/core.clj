(ns com.jayway.datomic.rps.core)

(defprotocol CommandHandler
  (perform [command state]))
