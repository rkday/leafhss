(defproject clojure-jdiameter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main clojure-jdiameter.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.mobicents.diameter/jdiameter-impl	"1.5.10.0-build639"]
                 [com.taoensso/timbre "3.0.0-RC2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.5.5"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [com.novemberain/welle "2.0.0-beta1"]
                 [org.clojure/data.xml "0.0.7"]]
  :plugins [[lein-cloverage "1.0.2"]]
  :profiles {:dev {:dependencies [[cljito "0.2.1"]
                                  [org.mockito/mockito-all "1.9.5"]]}})
