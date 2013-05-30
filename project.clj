(defproject domina_ci_jvm_executor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU Affero General Public License"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [
                 [clj-http "0.7.2"]
                 [clj-time "0.5.1"]
                 [compojure "1.1.5"]
                 [org.bouncycastle/bcpkix-jdk15on "1.48"]
                 [org.bouncycastle/bcprov-jdk15on "1.48"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-simple "1.7.3"]
                 [ring "1.1.8"] 
                 [ring/ring-jetty-adapter "1.1.8"]
                 ]
  :aot [domina-ci.executor.main]
  :main domina-ci.executor.main
  )
