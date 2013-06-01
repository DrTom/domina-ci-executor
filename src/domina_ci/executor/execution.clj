(ns domina-ci.executor.execution
  (:require 
    [clj-time.core :as time]
    [clojure.pprint :as pprint]
    [clojure.stacktrace :as stacktrace]
    [domina-ci.executor.core :as core]
    [domina-ci.executor.util :as util]
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
  last argument. Returns a map with the keys :process (value can be nil) and :thread 
  immediatelly.  The handler
  is always called from a different thread.  The handler takes one argument, a
  map with the keys: :exit-value, :stdout, and :stderr.

  Named options are: 
  * interpreter, an array of the command and arguments, defaults 
  to [\"cmd.exe\" \"/c\"] in windows and [\"bash\" \"-l\"] in unixes.
  * env-variables, an array of strings like [\"LEVEL=DEBUG\"], defaults to []
  * wokring-dir, defaults to the home directory of the user." 

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
          (let [process (. (Runtime/getRuntime) exec cmdarray envp working-dir) 
                thread (Thread. (fn []
                                  (try (do (.waitFor process)
                                           (handler 
                                             {:exit-value (.exitValue process)
                                              :stdout (slurp (.getInputStream process))
                                              :stderr (slurp (.getErrorStream process))
                                              }))
                                       (catch Exception e
                                         (handle-process-exception handler e)))))

                ]
            (.start thread)
            {:process process, :thread thread}))))
    (catch Exception e (let [thread  (Thread. #(handle-process-exception handler e))]
                         (.start thread)
                         {:process nil, :thread thread}
                         ))))

(defn swap-in-execution-params [uuid params]
  "Patches the current params for uuid with the given one."  
  (swap! core/executions
         (fn [executions uuid params]
           (let [current-execution-params (or (executions uuid) {})]
             (conj executions {uuid (conj current-execution-params params)})
             ))
         uuid params))

(defn create-and-process-execution [params result-handler]
  "Wraps around execute-script and places the params and later ammends with the
  result into core/executions under the uuid key.  Returns (almost)
  immediatelly and calls the result-handler with the result in a different
  thread."
  (let [uuid (:uuid params)
        params  (conj params {:started_at (util/date-time-to-iso8601 (time/now))
                              :state "executing"})]
    (swap-in-execution-params uuid params)
    (let [execution (execute-script 
                      (:script params)
                      (fn [result]
                        (let [ params-with-result (conj params result {:state (condp = (:exit-value result) 
                                                                                0 "successful" 
                                                                                "failed") 
                                                                       :finished_at (util/date-time-to-iso8601 (time/now))
                                                                       })]
                          (swap-in-execution-params uuid params-with-result)
                          (if result-handler (result-handler params-with-result))
                          )))] 
      execution)))

