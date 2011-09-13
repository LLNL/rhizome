(ns rhizome.turbotopics.app
  (:use [rhizome.turbotopics.util :only (pfhmap not-nil?)])
  (:use [rhizome.turbotopics.bigram :only (get-occur-counts
                                   valid-bigram?)])
  (:use [rhizome.turbotopics.ngram :only (recur-ngrams)])
  (:use [rhizome.turbotopics.algorithm :only (process-topic)])
  (:use [clojure.string :only (split)])
  (:use [clojure.contrib.seq-utils :only (indexed)])
  (:use [clojure.contrib.generic.functor :only (fmap)]))


;; Turbo Topics parameters
(def params {:minu 25
             :minv 10
             :thresh 10.82757})


;; Represents a single position within the corpus
(defrecord CorpusPosition [d z w])

(defn create-doc
  [vocabmapper [docidx docsample]]  
  (map #(CorpusPosition. %1 %2 %3)
       (repeat docidx)
       (:topics docsample)
       (map vocabmapper (:tokens docsample))))

(defn create-corpus
  [sample]
  (let [vocab (->> sample (mapcat :tokens) set vec)
        vocabmapper (into (hash-map) 
                          (for [[idx word] (indexed vocab)]
                            [word idx]))]
    {:positions (mapcat (partial create-doc vocabmapper)
                        (indexed sample))
     :vocab vocab}))

(defn tok-count
  "Count the number of tokens in a sig n-gram"
  [sb]
  (-> sb :ngram (split #"\s+") count))

;;
;; Reporting
;; 
(defn sort-by-score
  "Sort significant n-grams by like ratio score"
  [sbs]
  (reverse (sort-by :score sbs)))

(defn sort-by-count
  "Sort significant n-grams by frequency"
  [sbs]
  (reverse (sort-by :count sbs)))

(defn org-ngrams
  "Organize discovered n-grams for reporting" 
  [sig]
  (fmap sort-by-score (group-by tok-count sig)))

(defn ngram-report
  "Do report for a given count of n-gram"
  [ngrams]
  (apply
   str
   (interpose "\n"
              (map #(format "%f %d %s" (:score %1) (:count %1) (:ngram %1))
                   (take (min 25 (count ngrams))
                         ngrams)))))

(defn full-report
  "Do full report for all length n-grams"
  [sig]
  (let [orgd (org-ngrams sig)]
    (apply
     str
     (interpose "\n\n"
                (for [ct (-> orgd keys sort reverse)]
                  (ngram-report (orgd ct)))))))
                  
   
;;
;; Find bigrams only
;;
(defn do-bigram-z
  "Find and write out the SigBigrams for a single topic"
  [params vocab [z counts]]
  (println (format "Finding sig bigrams for topic %d" z))
  (full-report (process-topic params counts vocab)))

(defn do-bigrams
  "Find significant bigrams for each topic"
  [corpus]
  (println "Counting bigram occurrences...")
  (doall (pmap (partial do-bigram-z params (:vocab corpus))
               (get-occur-counts (:positions corpus)))))



;;
;; Find n-grams
;;
(defn do-ngrams
  "Recursively find significant n-grams for each topic"
  [corpus]
  (recur-ngrams params corpus))
