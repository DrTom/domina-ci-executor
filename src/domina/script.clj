; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.script
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    )
  (:require 
    [domina.exec :as exec]
    [clojure.tools.logging :as logging]
    ))


(set-logger! :level :info)

;; ### EXECUTOR AGENTS ########################################################

; handle prepare-executor scripts with an agent 
(defonce script-exec-agents (atom {}))

(defn script-exec-agent [id]
  "returns (and creates if necessary) the agent for the executor"
  (if-let [script-exec-agent (@script-exec-agents id)]
    script-exec-agent
    (do
      (swap! script-exec-agents 
             (fn [script-exec-agents id]
               (conj script-exec-agents {id (agent {}
                                                   :error-mode :continue)}))
             id)
      (@script-exec-agents id)
      )))

(defn use-memoized-or-execute [agent-state script]
  (let [res (agent-state (:name script))
        prev_state (:state res)]
    (if (= prev_state "success")
      agent-state
      (let [script-exec-result (exec/exec-script-for-params script)]
        (conj agent-state 
              {(:name script) 
               (select-keys script-exec-result
                            [:stderr :stdout :error :exit-status
                             :state :interpreter-command 
                             :started-at :finished-at])})))))

(defn memoized-executor-exec [script]
  (let [my-agent (script-exec-agent (:domina-execution-uuid script))]
    (send-off my-agent use-memoized-or-execute script)
    (await my-agent)
    (@my-agent (:name script))))

;; ###########################################################################

(defn process [scripts process-result] 
  (logging/info (str "processing scripts: " scripts))

  (loop [scripts scripts has-failures false]
    (if-let [script (first scripts)]
      (let [script-exec-result 

            (conj script 

                  (case (:type script)
                    "prepare_executor" (memoized-executor-exec script)

                    ("main" nil) (if (not has-failures)
                                   (exec/exec-script-for-params script)
                                   {:state "skipped" 
                                    :error "skipped because of previous failure"})

                    "clanup_executor" (do
                                        (logging/warn "TODO store and process cleanup-executor")
                                        {:state "success" 
                                         :stdout "Execution is deferred and might not be carried out at all."} )


                    {:state "failure"
                     :error (str "I don't know what to do with the type " (:type script) "\n" 
                                 "The following types are handled: " "main" "prepare_executor" "post_process" "cleanup-executor" "\n"
                                 "Undefined types will be handled like the main type."
                                 )}))]

                  (logging/debug "executed script: " script " with result: " script-exec-result)
                  (process-result script-exec-result)
                  (recur (rest scripts) 
                         (or has-failures  (not= "success" (:state script-exec-result))))))))

