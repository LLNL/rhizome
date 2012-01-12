(ns rhizome.turbotopics.permtest
    "Permutation and significance testing")

;; Arbitrary threshold (until permutations are much faster...)
;; 
(def threshold 25)

;;
;; Permutation test code
;;
(defn- sample-bigram-counts
  "Simulate bigram counts under a random permutation"
  [flatv sumu sumv]
  (frequencies (map flatv (take sumu (shuffle (range sumv))))))

(defn- freq-to-seq
  "Item-count dictionary to flat seq (pseudo-inverse of frequencies)"
  [dict]
  (vec (mapcat #(repeat (second %1) (first %1)) dict)))

(defn null-score
  "Max (over permutations) of max (over all candidate bigrams) score"
  [counts pval u v]
  (double threshold)) ;; Hacked over from chi-sq table in Blei's Python code...

;;
;; Doing actual permutation tests terribly slow!
;; TODO: fix that
;;
;;   (let [bigram-faker (partial sample-bigram-counts
;;                               (freq-to-seq nv-dict) sumu sumv)]
;;     (double (apply max (for [i (range (int (/ 1.0 p)))]
;;                          (apply max (map #(lr-score (bigram-faker)
;;                                                     nv-dict sumu sumv %1)
;;                                          (keys nv-dict))))))))
;; (def mnull-score (memoize null-score)) ;; memo-ized version

