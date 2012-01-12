(defproject rhizome "1.0.0-SNAPSHOT"
  :description "Learn and process LDA topics for use in IRIS system"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [solrclj/solrclj "0.1.1"]
                 [incanter "1.2.2"]
                 [congomongo "0.1.4-SNAPSHOT"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [token "1.0.0-SNAPSHOT"]
                 [semco "1.0.0-SNAPSHOT"]
                 [chisel "1.0.0-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.3.1"]
                     [marginalia "0.3.2"]]
  :main rhizome.app)
                 
