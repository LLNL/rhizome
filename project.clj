(defproject trecproc "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [solrclj/solrclj "0.1.1"]
                 [incanter "1.2.2"]
                 [clj-tagsoup "0.2.5"]
                 [token "1.0.0-SNAPSHOT"]
                 [turbotopics "1.0.0-SNAPSHOT"]
                 [semco "1.0.0-SNAPSHOT"]
                 [fs "0.2.0"]
                 [chisel "1.0.0-SNAPSHOT"]
                 [congomongo "0.1.4-SNAPSHOT"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :dev-dependencies [[swank-clojure "1.3.1"]]
  :main trecproc.app)
                 
