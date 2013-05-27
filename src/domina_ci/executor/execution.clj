(ns domina-ci.executor.execution
  (:require 
    [clojure.pprint :as pprint]
    [domina-ci.executor.core :as core]
    ))

(defn create-and-process-execution [params result-handler]
  (let [uuid (:uuid params)]
    (swap! core/executions 
           (fn [executions id params] (conj executions {id params}))
           uuid params)

    (let [
          process (. (Runtime/getRuntime) exec "ls -lah")
          stdout (slurp (.getInputStream process))
          stderr (slurp (.getErrorStream process))
          exit-value (.exitValue process)
          result {:state (if exit-value "success" "failed")
                  :command_stdout stdout
                  :command_stderr stderr}
          ]

;      (swap! core/executions
;             (fn [executions uuid result]
;               (conj executions {uuid (conj (executions uuid) result)})
;               uuid result))
;      
      )))
