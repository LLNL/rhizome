(ns trecproc.ingest
  (:use [trecproc.sgml :only (process-file)])
  (:use [fs :only (listdir join directory? file?)]))
       
(defmulti get-raw
  "Process TREC SGML file (or directory of same)"
  (fn [_ val] (cond (directory? val) ::trecdir
                    (file? val) ::trecfile)))

(defmethod get-raw ::trecfile
  [rootdir trecfile]
  (process-file (join rootdir trecfile)))

(defmethod get-raw ::trecdir
  [rootdir trecdir]
  (let [newroot (join rootdir trecdir)]
    (mapcat (partial get-raw newroot) (listdir newroot))))
          
(defmethod get-raw :default
  [rootdir _]
  (throw (Throwable. (format "Non-file non-dir in: %s" (str rootdir)))))
   
;;
;; Externally called function
;;
(defn get-raw-chunks
  "Do raw TREC ingest on a collection (eg, WSJ)"
  [rootdir] {:pre [(directory? rootdir)]}
  (map (partial get-raw rootdir) (listdir rootdir)))
