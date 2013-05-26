(defproject domina_ci_jvm_executor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GNU Affero General Public License"
            :url "http://www.gnu.org/licenses/agpl-3.0.html"}
  :dependencies [
                 [clj-http "0.7.2"]
                 [compojure "1.1.5"]
                 [org.clojure/clojure "1.5.1"]
                 [ring "1.1.8"] 
                 [ring/ring-jetty-adapter "1.1.8"]
                 ]
  :aot [domina-ci.executor.main]
  :main domina-ci.executor.main
  )
