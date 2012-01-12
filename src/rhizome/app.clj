(ns rhizome.app
  "Run topic model analysis on corpus in preparation for Iris"
  (:gen-class)
  (:use clojure.contrib.command-line)
  (:use [rhizome.related :only (get-related)])
  (:use [rhizome.lda :only (do-lda do-semco)])
  (:use [rhizome.turbo :only (get-ngrams)])      
  (:use [rhizome.mongo :only (mongo-connect)])
  (:use [rhizome.solr :only (get-all-solr-docs)])
  (:use [rhizome.stoplist :only (get-stop get-counts scan-counts)])
  (:use [somnium.congomongo :only (fetch with-mongo mass-insert!
                                         insert! add-index!)])
  (:require [clojure.string :as str]))

;;
;; Stopword generation
;;
(defn do-stop-counts
  "Emit hypothetical vocab sizes for different rare word thresholds"
  [solrconfig]
  (let [counts (-> solrconfig get-all-solr-docs get-counts)]
    (scan-counts counts (:stoplow solrconfig) (:stophigh solrconfig))))

(defn do-stop-list
  "Build stoplist of words occurring < thresh times"
  [solrconfig mongoconfig]
  (let [counts (-> solrconfig get-all-solr-docs get-counts)]
    (with-mongo (mongo-connect mongoconfig) 
      (mass-insert! :stop (map (partial hash-map :word)
                               (get-stop counts (:stopthresh solrconfig))))))
  (println "stop done!"))

;;
;; Turbo Topics
;;
(defn do-turbo
  "Do Turbo Topics to identify topical n-grams"
  [mongoconfig]
  (with-mongo (mongo-connect mongoconfig)
    (mass-insert! :ngram (get-ngrams (fetch :sample)))
    (add-index! :ngram [:topic])))

;;
;; Related topics
;;
(defn do-related
  "Generate topic-topic covariance values"
  [mongoconfig T]
  (with-mongo (mongo-connect mongoconfig)
    (mass-insert! :related (get-related (fetch :theta) T))
    (add-index! :related [:topic])))

(defn -main 
  "Main: read corpus from Solr, write LDA analysis out to MongoDB"
  [& args]
  (with-command-line args
    "LDA corpus processing for latent topic feedback"
    [[operation
      "Operation to do: count, stop, lda, turbo, related, semco (defaut: all)"
      "all"]
     [mongohost "MongoDB host" "localhost"]
     [mongoport "MongoDB port" "27017"]
     [mongoname "MongoDB database name" "topics"]     
     [solrhost "Solr index address" "localhost"]
     [solrport "Solr index port" "8983"]
     [solrfields "Comma-separated list of Solr fields to model" "title,text"]
     [solrtitle "Comma-separated list of Solr fields to model" nil]
     [stoplow "Low end of stoplist thresholds" "0"]
     [stophigh "High end of stoplist thresholds" "100"]
     [stopthresh "Filter out rare words occurring < stopthresh times" "50"]
     [T "Number of latent topics to use" "100"]
     [nsamp "Number of MCMC samples to take" "1000"]
     remaining]
    (let [solrconfig {:host solrhost :port (Integer/parseInt solrport)
                      :fields (str/split solrfields #",+")
                      :titlefield solrtitle
                      :stoplow (Integer/parseInt stoplow)
                      :stophigh (Integer/parseInt stophigh)
                      :stopthresh (Integer/parseInt stopthresh)}
          mongoconfig {:host mongohost
                       :port (Integer/parseInt mongoport)
                       :dbname mongoname}
          ldaparams {:T (Integer/parseInt T)
                     :nsamp (Double/parseDouble nsamp)}]
      (cond (= operation "count")
            (do-stop-counts solrconfig)
            (= operation "stop")
            (do-stop-list solrconfig mongoconfig)
            (= operation "lda")
            (do-lda mongoconfig ldaparams (get-all-solr-docs solrconfig))
            (= operation "turbo")
            (do-turbo mongoconfig)
            (= operation "related")
            (do-related mongoconfig (:T ldaparams))
            (= operation "semco")
            (do-semco mongoconfig (get-all-solr-docs solrconfig))
            (= operation "all")
            (do
              (do-stop-list solrconfig)
              (do-lda mongoconfig ldaparams (get-all-solr-docs solrconfig))
              (do-turbo mongoconfig)
              (do-related mongoconfig (:T ldaparams))
              (do-semco mongoconfig (get-all-solr-docs solrconfig))
            :else
            (println
             (format
              "Operation \"%s\" not understood, try -h for help" operation)))))))    
