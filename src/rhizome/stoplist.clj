(ns rhizome.stoplist
  "Stoplist construction utilities"
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
