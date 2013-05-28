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

(def defaul-system-interpreter
  (condp = (clojure.string/lower-case (System/getProperty "os.name"))
    "windows" ["cmd.exe" "/c"]
    ["bash" "-l"]))

(defn execute-script 
  [script handler & {:keys [working-dir env-variables interpreter] 
                     :or {env-variables [], 
                          interpreter defaul-system-interpreter
                          working-dir (java.io.File. (System/getProperty "user.home"))
                          }}]
  "Runs the script in the (default) interpreter by passing the full path as the
  last argument. Returns a process object (or nil) immediatelly.  The handler
  is always called from a different thread.  The handler takes one argument, a
  map with the keys: :exit-value, :stdout, and :stderr.

  Named options are: 
  * The interpreter, which is an array of the interpreter path and arguments, defaults 
  to [\"cmd.exe\" \"/c\"] in windows and [\"bash\" \"-l\"] in unixes.
  * env-variables, an array of strings like [\"LEVEL=DEBUG\"], defaults to []." 

  (try 
    (let [ script-file (File/createTempFile "domina_", ".script") ]
      (.deleteOnExit script-file)
      (spit script-file script)
      (let 
        [cmdarray (into-array (flatten (conj interpreter [(.getAbsolutePath script-file)])))
         envp (condp = (count env-variables)
                0 nil
                (into-array env-variables))]
        (try 
          (let [process (. (Runtime/getRuntime) exec cmdarray envp working-dir) ]
            (.start (Thread. (fn []
                               (try (do (.waitFor process)
                                        (handler 
                                          {:exit-value (.exitValue process)
                                           :stdout (slurp (.getInputStream process))
                                           :stderr (slurp (.getErrorStream process))
                                           }))
                                    (catch Exception e
                                      (handle-process-exception handler e))))))
            process))))
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
