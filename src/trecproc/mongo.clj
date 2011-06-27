;;
;; MongoDB functionality
;;
(ns trecproc.mongo
  (:use [clojure.contrib.duck-streams :only (read-lines)])
  (:use [somnium.congomongo :only (make-connection)]))         

;; The mongo.conf file in resources/ should contain
;; first line = collection name (eg, eco)
;; second line = host address (eg, 10.220.5.35)
(def configfile "mongo.conf")
(defn mongo-connect
  "Convenience function for estabilishing MongoDB connection"
  ([]    
     (let [configlines (read-lines
                        (.getResourceAsStream
                         (clojure.lang.RT/baseLoader)
                         configfile))]
       (mongo-connect (first configlines) (second configlines))))
  ([dbname hostaddr]  
     (make-connection dbname :host hostaddr)))
