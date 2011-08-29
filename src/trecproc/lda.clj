;;
;; Get LDA topic model from MALLET output text files,
;; do some processing, insert into MongoDB collection
;; 
(ns trecproc.lda
  (:use semco)
  (:use [chisel.phi :only (get-likely-phi)])
  (:use [chisel.theta :only (get-theta)])
  (:use [chisel.sample :only (get-sample)])
  (:use [chisel.lda :only (run-lda
                           write-topic-word
                           write-document-topic
                           write-sample
                           write-topics
                           write-documents)])                               
  (:use [chisel.instances :only (get-instance-list-from-iter
                                 document-to-instance)])
  (:use [token :only (*opennlp-stoplist* *opennlp-tokenizer*
                                         with-token process-text)])
  (:use [trecproc.mongo :only (mongo-connect)])
  (:use [trecproc.solr :only (get-all-solr-docs)])
  (:require [clojure.string :as str])
  (:use [somnium.congomongo :only (with-mongo distinct-values
                                   fetch insert! add-index! mass-insert!)]))

(defn make-inst
  [pt record]
  (println (:docid record))
  (document-to-instance [(:docid record) (str/join " " (pt (:text record)))]))

(defn get-dociter
  "Construct processed document map"
  []
  (with-token
    (binding [*opennlp-stoplist* (set
                                  (concat *opennlp-stoplist* 
                                          (map :word (fetch :stop))))]
      (map (bound-fn* (partial make-inst process-text))
           (get-all-solr-docs)))))

(defn do-lda
  "Do actual LDA run"
  [T numiter]
  (with-mongo (mongo-connect)
      (let [instances (get-instance-list-from-iter (get-dociter))
            topicmodel (run-lda instances T numiter)]
        (write-topics topicmodel "la.topics")
        (write-topic-word topicmodel "la.phi")
        (write-document-topic topicmodel "la.theta")
        (write-sample topicmodel "la.sample.gz")
        (write-documents instances "la.malletdocs")
        (mass-insert! :phi (get-likely-phi topicmodel))
        (add-index! :phi [:topic])
        (mass-insert! :sample (get-sample topicmodel))
        (add-index! :sample [:document])
        (mass-insert! :theta (get-theta topicmodel))
        (add-index! :theta [:document])        
      topicmodel)))

(defn get-topicwords
  [topic]
  (map :word (take 10 (reverse (sort-by :prob (:words topic))))))

(defn get-topics
  []
  (map get-topicwords (sort-by :topic (fetch :phi))))

(defn get-document
  [doc]
  (println (:docid doc))
  (:text doc))
  
(defn do-semco
  []
  (with-mongo (mongo-connect)
    (with-token
      (binding [*opennlp-stoplist* (set
                                    (concat *opennlp-stoplist* 
                                            (map :word (fetch :stop))))]    
        (mass-insert!
         :semco
         (semantic-coherence (get-topics)
                             (map (comp (bound-fn* process-text) get-document)
                                  (get-all-solr-docs))))))
    (add-index! :semco [:topic]))
  (println "semco done!"))
  
