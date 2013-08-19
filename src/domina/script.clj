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
    [domina.report :as report]
    ))


(set-logger! :level :info)

;; ### EXECUTOR AGENTS ########################################################

; handle prepare-executor scripts with an agent 
(def script-exec-agents (atom {}))

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
                            [:stderr :stdout :error :exit_status
                             :state :interpreter_command 
                             :started_at :finished_at])})))))

(defn memoized-executor-exec [script]
  (let [my-agent (script-exec-agent (:execution_sha1 script))]
    (send-off my-agent use-memoized-or-execute script)
    (await my-agent)
    (@my-agent (:name script))))

;; ###########################################################################

(defn process [scripts process-result] 
  (logging/info (str "processing scripts: " scripts))

  (loop [scripts scripts last-state "success"]
    (if-let [script (first scripts)]
      (let [script-exec-result (if (= last-state "success") 
                                 (conj script
                                   (if (:prepare_executor script)
                                     (memoized-executor-exec script)
                                     (exec/exec-script-for-params script)))
                                 (conj script 
                                       {:state "failed" 
                                        :error "skipped because of previous failure"}))]
        (logging/debug "executed script: " script " with result: " script-exec-result)
        (future (process-result script-exec-result))
        (recur (rest scripts) (:state script-exec-result ))))))

