(ns rhizome.turbo
  (:use [turbotopics.app :only (do-ngrams create-corpus)])
  (:require [clojure.string :as str]))        
 
(defn make-sig-ngram
  [sig]
  {:size (-> sig :ngram (str/split ,,, #"\s+") count)
   :score (:score sig) :ngram (:ngram sig) :count (:count sig)})

(defn make-topic-ngram
  [[topic sigseq]]
  {:topic topic :ngrams (map make-sig-ngram sigseq)})
      
(defn get-ngrams
  [sample]
  (map make-topic-ngram (do-ngrams (create-corpus sample))))
