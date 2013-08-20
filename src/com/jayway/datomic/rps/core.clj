(ns com.jayway.datomic.rps.core
  (:require [datomic.api :only [q db] :as d]))

(defprotocol CommandHandler
  (perform [command state]))
