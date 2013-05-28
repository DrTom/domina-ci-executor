(ns domina-ci.executor.execution
  (:require 
    [clojure.pprint :as pprint]
    [domina-ci.executor.core :as core]
    [clojure.stacktrace :as stacktrace]
    )
  (:import 
    [java.io File ])
  )

(defn- handle-process-exception [handler e]
  (handler 
    {:exit-value -1
     :stdout nil
     :stderr (with-out-str (stacktrace/print-stack-trace e))
     }))

(defn execute-script [interpreter script handler & more]
  "runs the script in the interpreter by passing the full path as the first
  argument; returns a process object (or nil) immediatelly ;  the handler is
  called from a different thread after the interpreter terminates; the handler
  takes one argument, a map with the keys :exit-value, :stdout, :stderr;" 
  (try 
    (let [ script-file (File/createTempFile "domina_", ".script") ]
      (pprint/pprint script-file)
      (.deleteOnExit script-file)
      (spit script-file script)
      (try 
        (let [process (. (Runtime/getRuntime) exec 
                         (into-array (flatten (conj interpreter [(.getAbsolutePath script-file)])))
                         (into-array ["X=Y"])
                         ) ]
          (.start (Thread. (fn []
                             (try (do (.waitFor process)
                                      (handler 
                                        {:exit-value (.exitValue process)
                                         :stdout (slurp (.getInputStream process))
                                         :stderr (slurp (.getErrorStream process))
                                         }))
                                  (catch Exception e
                                    (handle-process-exception handler e))))))
          process)))
      (catch Exception e (.start (Thread. #(handle-process-exception handler e))))))


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
