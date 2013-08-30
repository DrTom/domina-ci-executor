(defproject domina_executor "0.3.0"
  :description "Executor for the Domina CI System."
  :url "https://github.com/DrTom/domina-ci-executor"
  :license {:name "GNU Affero General Public License"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [
                 [clj-http "0.7.5"]
                 [clj-logging-config "1.9.10"]
                 [clj-time "0.5.1"]
                 [compojure "1.1.5"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail javax.jms/jms com.sun.jdmk/jmxtools com.sun.jmx/jmxri]]
                 [me.raynes/fs "1.4.4"]
                 [org.bouncycastle/bcpkix-jdk15on "1.48"]
                 [org.bouncycastle/bcprov-jdk15on "1.48"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [ring "1.1.8"] 
                 [ring/ring-jetty-adapter "1.1.8"]
                 [ring/ring-json "0.2.0"]
                 ]
  :aot [domina.main]
  :main domina.main 
  )
