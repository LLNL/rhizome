(ns trecproc.app
  (:gen-class)
  (:use clojure.contrib.command-line)
  (:use [incanter.core :only (matrix ncol sel)])
  (:use [incanter.stats :only (covariance)])
  (:use [trecproc.related :only (get-related)])
  (:use [trecproc.lda :only (do-lda do-semco)])
  (:use [trecproc.ingest :only (get-raw-chunks)])
  (:use [trecproc.turbo :only (get-ngrams)])      
  (:use [trecproc.mongo :only (mongo-connect)])
  (:use [trecproc.solr :only (get-all-solr-docs)])
  (:use [trecproc.stoplist :only (get-stop get-counts scan-counts)])
  (:use [somnium.congomongo :only (fetch with-mongo mass-insert!
                                         insert! add-index!)])
  (:use [clojure.contrib.seq-utils :only (indexed)])
  (:require [clojure.string :as str]))

;;
;; Raw ingest from TREC SGML files
;;
(def la "/home/andrzejewski1/projects/lat")

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
    (with-mongo (mongo-connect) (get-counts (get-all-solr-docs))))
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

;;
;; Related topics
;;
(defn do-related
  "Generate topic-topic covariance values"
  [T]
  (with-mongo (mongo-connect)
    (mass-insert! :related (get-related (fetch :theta) T))
    (add-index! :related [:topic])))

(defn -main 
  "Main: read corpus from Solr, write LDA analysis out to MongoDB"
  [& args]
  (with-command-line args
    "LDA corpus processing for latent topic feedback"
    [[solrhost "Solr index address " "localhost"]
     [solrport "Solr index port" "8080"]
     [stopthresh "Filter out rare words occurring < stopthresh times" "50"]
     [T "Number of latent topics to use" "100"]
     [nsamp "Number of MCMC samples to take" "1000"]
     remaining]
    (let [params {:solrhost solrhost
                  :solrport (Integer/parseInt solrport
                  :stopthresh (Integer/parseInt stopthresh),
                  :T (Integer/parseInt T),
                  :nsamp (Double/parseDouble nsamp)}]
      (do-stop-list (do-stop-counts) (:stopthresh params))
      (do-lda (:T params) (:nsamp params))
      (do-turbo)
      (do-related (:T params))
      (do-semco))))

      
