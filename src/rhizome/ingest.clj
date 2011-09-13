(ns rhizome.ingest
  (:use [rhizome.sgml :only (process-file)])
  (:use [fs :only (listdir join directory? file?)]))
       
(defmulti get-raw
  "Process TREC SGML file (or directory of same)"
  (fn [rootdir val] (cond (directory? (join rootdir val)) ::trecdir
                          (file? (join rootdir val)) ::trecfile)))

(defmethod get-raw ::trecfile
  [rootdir trecfile]
  (process-file (join rootdir trecfile)))

(defmethod get-raw ::trecdir
  [rootdir trecdir]
  (let [newroot (join rootdir trecdir)]
    (mapcat (partial get-raw newroot) (listdir newroot))))
          
(defmethod get-raw :default
  [rootdir unkn]
  (println (format "rootdir = %s, unkn = %s" rootdir unkn))
  (throw (Throwable. (format "Non-file non-dir in: %s" (str rootdir)))))
   
;;
;; Externally called function
;;
(defn get-raw-chunks
  "Do raw TREC ingest on a collection (eg, WSJ)"
  [rootdir] {:pre [(directory? rootdir)]}
  (map (partial get-raw rootdir) (listdir rootdir)))
