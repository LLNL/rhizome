(ns rhizome.turbotopics.bigram
  "Functions for identifying and counting bigrams"
  (:use [rhizome.turbotopics.util :only (pfhmap not-nil?)])
  (:use [clojure.contrib.generic.functor :only (fmap)]))

;; Occurrence counts
(defrecord OccurCounts [nuv nv nu sumv])

;; Observed bigram
(defrecord Bigram [w1 w2 z])

(defn valid-bigram?
  "Is this a valid topic bigram (both exist, same topic, same doc)"
  [[pos1 pos2]]
  (and (not-nil? pos1)
       (not-nil? pos2)
       (== (:z pos1) (:z pos2))
       (== (:d pos1) (:d pos2))))
  
(defn- make-bigram
  "Convert two corpus positions to Bigram"
  [[pos1 pos2]]
  (Bigram. (:w pos1) (:w pos2) (:z pos1)))

(defn- get-bigrams
  "Convert seq of positions to seq of topic-bigrams"
  [positions]
  (map make-bigram (filter valid-bigram? (partition 2 1 positions))))
                           
(defn count-topic-bigrams
  "Get Bigram counts for each topic"
  [bigrams]
  (pfhmap frequencies (group-by :z bigrams)))
     
(defn add-nil
  "Add a to b, replacing b with 0 if b is nil"
  [a b]
  (if (nil? a)
    b
    (+ a b)))

(defn update-counts
  "Update our OccurCounts data struct to reflect a new observation" 
  [counts u v count]
  (-> counts
      (update-in ,,, [:nuv u v] add-nil count)
      (update-in ,,, [:nv v] add-nil count)
      (update-in ,,, [:nu u] add-nil count)      
      (update-in ,,, [:sumv] add-nil count)))

(defn count-occur
  "Convert bigram counts to u->v->N(v|u) and N(v|*) count dictionaries"
  [argbigrams]
  (loop [[b count] (first argbigrams)
         bigrams (rest argbigrams)
         counts (OccurCounts. (hash-map) (hash-map) (hash-map) 0)]
    (if (nil? (:w1 b))
      counts
      (recur
       (first bigrams)
       (rest bigrams)
       (update-counts counts (:w1 b) (:w2 b) count)))))


(defn get-occur-counts
  "EXTERNALLY CALLED: get counts used for Turbo Topics"
  [positions]
  (pfhmap count-occur (count-topic-bigrams (get-bigrams positions))))
