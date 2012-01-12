(ns rhizome.turbotopics.util
  "Utility functions"
  (:use [clojure.contrib.string :only (blank?)]))

(def not-nil? (comp not nil?))

(def not-blank? (comp not blank?))

(defn pfhmap
  "Poorly named pmap version of fmap, but only for hash-map"
  [f hm]
  (into (hash-map)
        (pmap #(vector (first %1) (f (second %1)))              
              hm)))

(defn pmapcat
  "Parallel mapcat"
  [f vs]
  (apply concat (pmap f vs)))
