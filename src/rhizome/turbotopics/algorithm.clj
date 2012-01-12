(ns rhizome.turbotopics.algorithm
  "The actual Turbo Topics (style) algorithm"
  (:use [rhizome.turbotopics.util :only (not-nil?)])
  (:use [rhizome.turbotopics.permtest :only (null-score)])
  (:use [clojure.set :only (union)])
  (:use [clojure.contrib.seq-utils :only (indexed)])
  (:use [clojure.contrib.generic.math-functions :only (log exp)]))


;; Occurrence counts
(defrecord OccurCounts [nuv nv nu sumv])

;; A significant bigram
(defrecord SigBigram [ngram count score])

(defn- corpus-unigram
  "Count topic unigrams"
  [topic positions]
  (frequencies (map :w (filter #(== topic (:z %1)) positions))))

                                    
;;
;; Turbo topics - log-likelihood ratio score of adding new word
;;
(defn- safelog
  "Avoid NaN contamination of our calculations"
  [val]
  (if (zero? val)
    (double -100000000)
    (log val)))

(defn- lr-score
  "Given counts and u, calculate score of v"
  [counts u v]
  (if (zero? (-> counts :nuv (get u) (get v 0)))
    (double -100000000)
    (let [;; Pre-fetch the necessary counts 
          nuv (-> counts :nuv (get u) (get v))
          nu (-> counts :nu (get u))
          nv (-> counts :nv (get v))
          sumv (:sumv counts)
          
          ;; P(v|u) under new
          lpi-vu-new (safelog (/ nuv nu))

          ;; P(v ~| u) under new
          lpi-v-new (- (safelog (- nv nuv))
                       (safelog (- sumv nu)))

          ;; P(v) old
          lpi-v-old (safelog (/ nv sumv))
        
          ;; P(v') - SHOULD THIS BE UPDATED - I THINK SO...?
          ln2 (safelog (- 1 (exp lpi-v-new)))
          ld2 (safelog (- 1 (exp lpi-v-old)))
          lgamma2 (- ln2 ld2)

          ;; gamma_u new scaling factor
          ;;
          ;; If we believe P(v') should be updated for v' != v,
          ;; then should mult old-style gamma_u by new gamma_{not u}
          ;;
          ;; (1 - pi-v-new) terms cancel, leaving 1 - pi-v-old in denom
          ;;
          lnumer (safelog (- 1 (exp lpi-vu-new)))
          ;;ldenom (safelog (- 1 (exp lpi-v-new)))
          ldenom (safelog (- 1 (exp lpi-v-old)))
          lgamma (- lnumer ldenom)]                

      ;; Calculate and return actual log-odds ratio
      (+ (* nuv lpi-vu-new)  ;; bigram occur
         (* (- nv nuv) lpi-v-new) ;; unigram occur
         (* (- nu nuv) lgamma)
         ;; re-normalized uv' for v' != v
         (* (- (- sumv nv) nu)
            lgamma2) ;; re-normalized v' for v' != v and not u
         (* -1 nv lpi-v-old))))) ;; previous unigram occur

(defn root-candidates
  "Any root term occuring > minu times is a bigram root candidate"
  [counts minu]
  (filter #(> (-> counts :nu (get %1 0)) minu)
          (keys (:nu counts))))

(defn second-candidates
  "Second terms must occur > minv times to be bigram second candidates"
  [counts u minv]
  (filter #(> (-> counts :nuv (get u) (get %1 0)) minv)
          (keys (-> counts :nuv (get u)))))
  
(defn- get-sig-bigrams
  "For a given u, get significant v according to like ratio (eq4)"
  [params counts u]
  (let [candterms (second-candidates counts u (:minv params))
        scorer (partial lr-score counts u)
        sighits (filter #(> (scorer %1) (:thresh params))
                        candterms)]
    sighits))

(defn- process-root-word
  "Generate significant uv bigrams from root word u"
  [params counts u]
  (set (map #(vector u %1) (get-sig-bigrams params counts u))))

(defn process-topic
  "EXT CALLED: for a topic, find significant bigrams and scores (uses pmap)"
  [params counts vocab]
  (let [rootcand (root-candidates counts (:minu params)) ;; candidate root word
        sigbigrams (apply union (pmap #(process-root-word params counts %1)
                                      rootcand))]
    (for [[u v] sigbigrams]
      (SigBigram.
       (str (vocab u) " " (vocab v))
       (-> counts :nuv (get u) (get v))
       (lr-score counts u v)))))

