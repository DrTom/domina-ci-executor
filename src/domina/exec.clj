; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.exec
  (:import 
    [java.io File]
    [java.util UUID]
    )
  (:require
    [clj-commons-exec :as commons-exec]
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

;(set-logger! :level :debug)
;(clojure.pprint/pprint @(commons-exec/sh ["bash" "-l" "-c" "load_rbenv && rbenv shell ruby-2.0.0 && ruby -v"] {:env (System/getenv) }))
;(clojure.pprint/pprint @(commons-exec/sh ["sh" "-l" "-c" "bash -l -c \"env |sort\""] {:env {}}))
;(shell/sh "bash" "-l" "-c" "env" :env {})



(def ^:private defaul-system-interpreter
  (condp = (clojure.string/lower-case (System/getProperty "os.name"))
    "windows" ["cmd.exe" "/c"]
    ["bash" "-l"]))

(defn ^:private exec-script 
  [script & {:keys [timeout working-dir env-variables interpreter]}]
  "Runs the script in the (default) interpreter by passing the full path as the
  last argument. Blocks until the script exits or times out."
  (let [timeout (or timeout 200)
        script-file (File/createTempFile "domina_", ".script") 
        env-variables (or env-variables {})
        interpreter (or interpreter defaul-system-interpreter)]
    (logging/info (str "exec-script" (reduce (fn [s x] (str s " # " x)) [script timeout env-variables interpreter working-dir])))
    (.deleteOnExit script-file)
    (spit script-file script)
    (.setExecutable script-file true)
    (let 
      [command (conj interpreter (.getAbsolutePath script-file))
       res (deref (commons-exec/sh command 
                                   {:env (conj {} (System/getenv) env-variables)
                                    :dir working-dir  
                                    :watchdog (* 1000 timeout)}))]
      res)))


(defn ^:private prepare-env-variables [{ex-uuid :domina-execution-uuid trial-uuid :domina-trial-uuid :as params}]
  (logging/debug ":domina-execution-uuid " ex-uuid ":domina-trial-uuid " trial-uuid)
  (util/upper-case-keys 
    (util/rubyize-keys
      (conj params {:domina-trial-int (util/uuid-to-short trial-uuid)
                    :domina-execution-int (util/uuid-to-short ex-uuid)
                    }))))

(defn exec-script-for-params [params]
  (logging/info (str "exec-script-for-params" (select-keys params [:name])))
  (try
    (let [started {:started-at (time/now)}
          env-variables (prepare-env-variables (:env-vars params))
          working-dir (:working-dir params)
          exec-res (exec-script (:body params) 
                                :working-dir working-dir 
                                :env-variables env-variables 
                                :timeout (:timeout params))] 
      (conj params 
            started 
            {:finished-at (time/now)
             :exit-status (:exit exec-res)
             :state (condp = (:exit exec-res) 
                      0 "success" 
                      "failed")
             :stdout (:out exec-res)
             :stderr (:err exec-res) 
             :error (:error exec-res)
             :interpreter-command (:intepreter exec-res)
             }))
    (catch Exception e
      (do
        (logging/error (with-out-str (print-stack-trace e)))
        (conj params
              {:state "failed"
               :error (with-out-str (print-stack-trace e))
               })))))
