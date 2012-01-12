(ns rhizome.solr
  "Solr index interactions"
  (:use [solrclj :only (add query delete-by-query commit optimize)])
  (:use [solrclj.servers :only (create-solr-server)])
  (:require [clojure.string :as str]))

(defn get-string-from-array
  "Extract the string result from a Solr Java array restur value"
  [arrayarg]
  (if (and ((comp not nil?) arrayarg)
           ((comp not zero?) (.size arrayarg)))
    (.get arrayarg 0)
    ""))

(defn get-text-fields
  "Get the desired text fields from a Solr document"
  [fields solrdoc]
  (str/join " " (map #(get-string-from-array ((keyword %1) solrdoc))
                     fields)))

(defn make-mongo-map
  "Convert a single Solr query result to a Mongo-style map"
  [solrconfig solrdoc]
  (if (nil? solrdoc)
    nil
    {:text (get-text-fields (:fields solrconfig) solrdoc)
     :title (if (:titlefield solrconfig)
              (get-text-fields [(:titlefield solrconfig)] solrdoc)
              (:id solrdoc))
     :docid (:id solrdoc)}))

(defn get-solr-server
  "Instantiate the Solr server connection"
  [config]
  (create-solr-server (assoc config :type :http)))

(defn get-solr-docs
  "Solr query wrapper"
  [solrserver querystr & queryargs]
  (get-in (apply query solrserver querystr queryargs) [:response :docs]))

(def CHUNKSIZE 1000)                       
(defn get-all-solr-docs
  "Lazily fetch every document in the index, fetching CHUNKSIZE docs as needed"
  ([solrconfig] (get-all-solr-docs solrconfig (get-solr-server solrconfig) 0))
  ([solrconfig solrserver startidx]
     (let [curdocs (get-solr-docs solrserver "*:*"
                                  :start startidx :rows CHUNKSIZE)]
       (if (empty? curdocs)
         []         
         (concat
          (map (partial make-mongo-map solrconfig) curdocs)
          (lazy-seq (get-all-solr-docs solrconfig solrserver
                                       (+ startidx CHUNKSIZE))))))))
