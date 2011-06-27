(ns trecproc.stoplist
  (:use [token :only (with-token process-text count-tokens)]))

(defn echo-count
  "Count tokens within a :raw document, echoing :docid"
  [record]
  (println (:docid record))
  (-> record :text count-tokens))

(defn scan-counts
  "Print hypothehtical vocab sizes for diff stoplist thresholds"
  [counts tstart tstop]
  (doseq [thresh (range tstart tstop)]
    (println (format "Thresh %d = %d words" thresh
                     (count (filter #(> (second %1) thresh) counts))))))
                                    
(defn get-stop
  "Get words occurring < thresh times"
  [counts thresh]
  (map first (filter #(< (second %1) thresh) counts)))

(defn get-counts
  "Get token counts over entire corpus"
  [rawdocs]
  (with-token (reduce (partial merge-with +) (map echo-count rawdocs))))


;; ;;
;; ;; DEBUG: check code for interactive verification
;; ;; of stopword list issues
;; ;;
;; ;; (bugs with tokens stoplist binding are letting stopwors through)
;; ;;
;; (defn generate-check-cts
;;   [topics]
;;   (def cts   (with-mongo (mongo-connect)
;;     (with-token
;;       (binding [*opennlp-stoplist* (set
;;                                     (concat *opennlp-stoplist* 
;;                                             (map :word (fetch :stop))))]    
;;         (get-counts topics                              (map (comp (bound-fn* process-text) get-document)
;;                                                              (fetch :raw))))))))

;; (defn check-counts
;;   [topics]
;;   (doseq [t topics]
;;     (if (not (every? (partial contains? (:counts t)) (:words t)))
;;       (do
;;         (println (:topic t))
;;         (println (:words t))))))
