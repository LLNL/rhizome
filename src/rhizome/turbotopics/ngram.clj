(ns rhizome.turbotopics.ngram
  "Functions for recursively identifying n-grams"
  (:use [clojure.contrib.seq-utils :only (indexed)])
  (:use [rhizome.turbotopics.util :only (pfhmap pmapcat not-nil?)])
  (:use [rhizome.turbotopics.bigram :only (get-occur-counts
                             valid-bigram?)])
  (:use [rhizome.turbotopics.algorithm :only (process-topic)]))

;; Represents a single position within the corpus
(defrecord CorpusPosition [d z w])

(defn pospair-to-newterm
  "Form a new term from a pair of corpus positions"
  [vocab p1 p2]
  (format "%s %s"
          (-> p1 :w vocab)
          (-> p2 :w vocab)))

(defn position-hit?
  "Is this pair of positions a newly found significant bigram?"
  [p1 p2 vocab vocab2idx]
  (contains? vocab2idx (pospair-to-newterm vocab p1 p2)))

(defn get-new-terms
  "Get all new terms from found, which maps z to SigBigrams"
  [found]
  (vec (set (mapcat (partial map :ngram) (vals found)))))

(defn transform-vocab
  "Augment the vocabulary with newly found significant bigrams"
  [vocab found]        
  (vec (concat vocab (get-new-terms found))))

(defn transform-positions
  "Transform the corpus positions to take new n-grams into account"
  [vocab positions]
  (let [vocab2idx
        (into (hash-map) (for [[idx term] (indexed vocab)] [term idx]))]
    (loop [prev nil
           cur (first positions)
           remaining (rest positions)
           newpositions (vector)]
      (if (and (nil? cur) (nil? prev))
        newpositions ;; we're done
        (if (nil? prev) ;; either at start, or earlier symbol was bigram
          (recur cur (first remaining) (rest remaining) newpositions)
          (if (and (valid-bigram? [prev cur])
                   (position-hit? prev cur vocab vocab2idx)) ;; bigram hit
            (recur nil (first remaining) (rest remaining)
                   (conj newpositions
                         (CorpusPosition.
                          (:d cur) (:z cur)
                          (vocab2idx (pospair-to-newterm vocab prev cur)))))
            (recur cur (first remaining) (rest remaining)
                   (conj newpositions prev)))))))) ;; not a hit, emit prev
            
(defn transform-corpus
  "Augment vocabulary with new n-grams and transform the corpus"
  [corpus found]
  (let [newvocab (transform-vocab (:vocab corpus) found)]
    {:vocab newvocab,
     :positions (transform-positions newvocab (:positions corpus))}))
  
(defn recur-ngrams
  "Recursively find n-grams"
  [params argcorpus]  
  (loop [corpus argcorpus
         prevfound (hash-map)]
    (let [found (pfhmap #(process-topic params %1 (:vocab corpus))
                        (get-occur-counts (:positions corpus)))]
      (if (zero? (reduce + (map count (vals found)))) ;; no new n-grams, quit
        prevfound
        (recur
         (transform-corpus corpus found)
         (merge-with concat prevfound found))))))
