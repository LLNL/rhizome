(ns rhizome.mongo
  "MongoDB functionality"
  (:use [somnium.congomongo :only (make-connection)]))         

(defn mongo-connect
  "Convenience function for estabilishing MongoDB connection"
  [mongoconfig]
  (apply make-connection ((juxt :dbname :host :port) mongoconfig)))
