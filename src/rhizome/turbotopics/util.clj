(comment

  Utility functions
  
  )

(ns rhizome.turbotopics.util
  (:use [clojure.contrib.string :only (blank?)]))

(defn not-nil?
  "Gets used often enough to justify..."
  [v]
  (not (nil? v)))

(defn not-blank?
  "Gets used often enough to justify..."
  [s]
  (not (blank? s)))

(defn pfhmap
  "Poorly named pmap version of fmap, but only for hash-map"
  [f hm]
  (into (hash-map)
        (pmap #(vector (first %1) (f (second %1)))              
              hm)))

(defn pmapcat
  "Parallel version of mapcat"
  [f vs]
  (apply concat (pmap f vs)))
