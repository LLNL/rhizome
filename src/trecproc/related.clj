(ns trecproc.related
  (:use [clojure.contrib.combinatorics :only (combinations)])
  (:use [incanter.core :only (matrix ncol sel)])
  (:use [incanter.stats :only (covariance)]))
 
(defn calc-covar
  [mat i j]
  (covariance (sel mat true i) (sel mat true j)))
              
(defn make-related-entries
  [mat ij]
  (let [i (first ij)
        j (second ij)
        cov (calc-covar mat i j)]
    (println (format "i=%03d j=%03d" i j))
    (list 
     {:topic i :cotopic j :covar cov}
     {:topic j :cotopic i :covar cov})))

(defn make-row
  "Convert sparse document-theta map to a dense vector"
  [T doctopic]
  (println (:document doctopic))
  (apply assoc (vec (repeat T 0.0)) (mapcat (juxt :topic :prob)
                                            (:topics doctopic))))

(defn get-related
  [thetarows T]
  (let [mat (matrix (map (partial make-row T) thetarows))]
    (apply concat 
           (pmap (partial make-related-entries mat)                 
                 (combinations (range T) 2)))))
          
  
