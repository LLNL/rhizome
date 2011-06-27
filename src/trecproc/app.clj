(ns trecproc.app
  (:gen-class)
  (:use [trecproc.lda :only (do-lda do-semco)])
  (:use [trecproc.ingest :only (get-raw-chunks)])
  (:use [trecproc.turbo :only (get-ngrams)])      
  (:use [trecproc.mongo :only (mongo-connect)])
  (:use [trecproc.stoplist :only (get-stop get-counts)])
  (:use [somnium.congomongo :only (fetch with-mongo mass-insert!
                                         insert! add-index!)])
  (:use [clojure.contrib.seq-utils :only (indexed)])
  (:require [clojure.string :as str]))

;;
;; Raw ingest
;;
(def la "/home/andrzejewski1/projects/summer-proj/la")

(defn do-ingest
  [trecdir]
  (with-mongo (mongo-connect)
    (doseq [chunk (get-raw-chunks trecdir)]
      (mass-insert! :raw chunk))
    (add-index! :raw [:docid])))

;;
;; Stopword generation
;;
(defn do-stop-counts
  []
  (def stopcounts 
    (with-mongo (mongo-connect) (get-counts (fetch :raw))))
  (scan-counts stopcounts 50 150))

(defn do-stop-list
  "Build stoplist of words occurring < thresh times"
  [counts thresh]
  (with-mongo (mongo-connect) 
    (mass-insert! :stop (map (partial hash-map :word)
                             (get-stop counts thresh))))
  (println "stop done!"))

;;
;; Turbo Topics
;;
(defn do-turbo
  "Do Turbo Topics to identify topical n-grams"
  []
  (with-mongo (mongo-connect)
    (mass-insert! :ngram (get-ngrams (fetch :sample)))
    (add-index! :ngram [:topic])))

;; (do-lda 500 1000)
;; (do-semco)


(defn -main
  []
  (println "either change this and rebuild, or launch from repl/swank"))
