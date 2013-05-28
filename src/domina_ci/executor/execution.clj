(ns domina-ci.executor.execution
  (:require 
    [clojure.pprint :as pprint]
    [domina-ci.executor.core :as core]
    )
  (:import 
    [java.io File ])
  )

(defn execute-script [command script handler]
  (let [ script-file (File/createTempFile "domina_", ".script") ]
    (pprint/pprint script-file)
    (.deleteOnExit script-file)
    (spit script-file script)
    (let [process (. (Runtime/getRuntime) exec 
                     (into-array [command (.getAbsolutePath script-file)])) ]
      (.start (Thread. (fn []
                         (.waitFor process)
                         (handler 
                           {:exit-value (.exitValue process)
                            :stdout (slurp (.getInputStream process))
                            :stderr (slurp (.getErrorStream process))
                            }))))
      process
      )))

(defn create-and-process-execution [params result-handler]
  (let [uuid (:uuid params)]
    (swap! core/executions 
           (fn [executions id params] (conj executions {id params}))
           uuid params)

    (let [
          process (. (Runtime/getRuntime) exec (:command params))
          stdout (slurp (.getInputStream process))
          stderr (slurp (.getErrorStream process))
          exit-value (.exitValue process)
          result {:state (if exit-value "success" "failed")
                  :command_stdout stdout
                  :command_stderr stderr}
          ]

      (swap! core/executions
             (fn [executions uuid result]
               (conj executions {uuid (conj (executions uuid) result)}))
               uuid result)
      
      )))
