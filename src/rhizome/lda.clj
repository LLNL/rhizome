(ns rhizome.lda
  "Code for running LDA and doing topic semantic coherence analysis"
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
  (:use [rhizome.mongo :only (mongo-connect)])
  (:require [clojure.string :as str])
  (:use [somnium.congomongo :only (with-mongo distinct-values
                                   fetch insert! add-index! mass-insert!)]))

(defn make-inst
  "Construct MALLET instance from a single document"
  [pt record]
  (println (:docid record))
  (document-to-instance [(:docid record) (str/join " " (pt (:text record)))]))

(defn get-dociter
  "Construct processed document map"
  [solrdocs]
  (with-token
    (binding [*opennlp-stoplist* (set
                                  (concat *opennlp-stoplist* 
                                          (map :word (fetch :stop))))]
      (map (bound-fn* (partial make-inst process-text))
           solrdocs))))

(defn do-lda
  "Do the actual LDA run"
  [mongoconfig ldaparams solrdocs]
  (with-mongo (mongo-connect mongoconfig)
      (let [instances (get-instance-list-from-iter (get-dociter solrdocs))
            topicmodel (run-lda instances
                                :T (:T ldaparams)
                                :numiter (:nsamp ldaparams))]
        (write-topics topicmodel "output.topics")
        (write-topic-word topicmodel "output.phi")
        (write-document-topic topicmodel "output.theta")
        (write-sample topicmodel "output.sample.gz")
        (write-documents instances "output.malletdocs")
        (mass-insert! :phi (get-likely-phi topicmodel))
        (add-index! :phi [:topic])
        (mass-insert! :sample (get-sample topicmodel))
        (add-index! :sample [:document])
        (mass-insert! :theta (get-theta topicmodel))
        (add-index! :theta [:document])        
      topicmodel)))

(defn get-topicwords
  "Extract the Top N topic words"
  ([topic]
     (get-topicwords topic 10))
  ([topic N]
     (map :word (take N (reverse (sort-by :prob (:words topic)))))))

(defn get-topics
  "Get Top N words from all topics"
  []
  (map get-topicwords (sort-by :topic (fetch :phi))))

(defn get-document
  "Get the raw text of a document"
  [doc]
  (println (:docid doc))
  (:text doc))
  
(defn do-semco
  "Do semantic coherence analysis of topics"
  [mongoconfig solrdocs]
  (with-mongo (mongo-connect mongoconfig)
    (with-token
      (binding [*opennlp-stoplist* (set
                                    (concat *opennlp-stoplist* 
                                            (map :word (fetch :stop))))]    
        (mass-insert!
         :semco
         (semantic-coherence (get-topics)
                             (map (comp (bound-fn* process-text) get-document)
                                  solrdocs)))))
    (add-index! :semco [:topic]))
  (println "semco done!"))
  
