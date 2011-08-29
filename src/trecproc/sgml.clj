;;
;; Parse TREC SGML file
;;
(ns trecproc.sgml
  (:require [clojure.string :as str])
  (:use [swank.core :only (break)])
  (:use [pl.danieljanus.tagsoup :only (parse-string)])
  (:use [clojure.contrib.duck-streams :only (slurp*)]))

(defn wrap-file
  "TREC SGML hack: need a top-level tag for well-formed HTML"
  [filename]
  (str "<DOCS>" (slurp* filename) "</DOCS>"))

(defn process-p
  "Process a P field"
  [[tag attr content]]
  (if (or (= tag :p) (= tag :P))
    content
    '()))

(defn process-text
  "Process a TEXT field"
  [content]
  (if (string? (first content))
    {:text (first content)}
    {:text (str/join " " (map process-p content))}))

(defn process-content
  "We only care about DOCNO and TEXT fields"
  [[tag attr & content]]
  (cond (= tag :DOCNO) {:docid (str/trim (first content))}
        (= tag :HEADLINE) {:title (str/trim (:text (process-text content)))}
        (= tag :TEXT) (process-text content)))
        
(defn process-doc
  "Process a single DOC entry"
  [[doctag docattr & doccontent]]    
  (reduce (partial merge-with concat) (map process-content doccontent)))

(defn process-file
  "Return a seq of document hashmaps"
  [filename]
  (println filename)
  (->> filename wrap-file parse-string              
       rest rest (map process-doc)
       (filter (comp not nil? :text))))
