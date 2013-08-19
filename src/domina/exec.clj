; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.exec
  (:import 
    [java.io File]
    [java.util UUID]
    )
  (:require
    [clj-time.core :as time]
    [clojure.string :as string]
    [clojure.tools.logging :as logging]
    [domina.util :as util]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    [clojure.java.shell :as shell]
    [clojure.stacktrace :only (print-stack-trace)]
    ))


(def defaul-system-interpreter
  (condp = (clojure.string/lower-case (System/getProperty "os.name"))
    "windows" ["cmd.exe" "/c"]
    ["bash" "-c"]))

(defn exec-script 
  [script & {:keys [timeout working-dir env-variables interpreter] 
             :or {env-variables {}, 
                  timeout 200
                  interpreter defaul-system-interpreter
                  working-dir (System/getProperty "user.home")
                  }}]
  "Runs the script in the (default) interpreter by passing the full path as the
  last argument. Returns a map with the keys :process (value can be nil) and
  :thread immediatelly.  

  Named options are: 
  * interpreter, an array of the command and arguments, defaults 
  to [\"cmd.exe\" \"/c\"] in windows and [\"bash\" \"-l\"] in unixes.
  * env-variables, a hash of env variables  {:LEVEL \"DEBUG\"}, defaults to {}
  * working-dir, defaults to the home directory of the user." 

  (logging/info (str "exec-script" (reduce (fn [s x] (str s " # " x)) [script timeout interpreter working-dir])))
  (let [ script-file (File/createTempFile "domina_", ".script") ]
    (.deleteOnExit script-file)
    (spit script-file script)
    (.setExecutable script-file true)
    (let 
      [command (conj interpreter (str "bash -l " (.getAbsolutePath script-file)))
       extended-env-variables  (conj {} (System/getenv) env-variables) 
       command-with-options (conj command :env extended-env-variables :dir working-dir)
       res (future-call #(apply shell/sh command-with-options)) 
       timeout-ms (* 1000 timeout) 
       ret-val (deref res timeout-ms {:exit -1 :out nil :err nil :error "timeout"})
       extended-ret-val (conj ret-val {:interpreter_command command})
       ]
      (logging/info (str "extended-env-variables " extended-env-variables))
      (logging/info (str "exec-script returns: " extended-ret-val))
      extended-ret-val
      )))


(defn prepare-env-variables [{ex-uuid :domina_execution_uuid trial-uuid :domina_trial_uuid :as params}]
  (util/upper-case-keys 
    (conj params {:domina_trial_int (util/uuid-to-short trial-uuid)
                  :domina_execution_int (util/uuid-to-short ex-uuid)
                  })))

(defn exec-script-for-params [params]
  (logging/info (str "exec-script-for-params" (select-keys params [:name])))
  (try
    (let [started {:started_at (util/date-time-to-iso8601 (time/now))}
          env-variables (prepare-env-variables params)
          working-dir (:working_dir params)
          timeout (or (:timeout params) 200)
          exec-res (exec-script (:body params) :working-dir working-dir :env-variables env-variables :timeout timeout)]

      (conj params 
            started 
            {:finished_at (util/date-time-to-iso8601 (time/now))
             :exit_status (:exit exec-res)
             :state (condp = (:exit exec-res) 
                      0 "success" 
                      "failed")
             :stdout (:out exec-res)
             :stderr (:err exec-res) 
             :error (:error exec-res)
             :interpreter_command (:intepreter exec-res)
             }
            ))
    (catch Exception e
      (do
        (logging/error (with-out-str (print-stack-trace e)))
        (conj params
              {:state "failed"
               :error (with-out-str (print-stack-trace e))
               })))))



