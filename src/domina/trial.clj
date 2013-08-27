; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.
 
(ns domina.trial
  (:require
    [clojure.pprint :as pprint]
    [clojure.stacktrace :as stacktrace]
    [clojure.tools.logging :as logging]
    [domina.exec :as exec]
    [domina.git :as git]
    [domina.reporter :as reporter]
    [domina.script :as script]
    [domina.util :as util]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

(set-logger! :level :info)

(defonce report-agents (atom {}))

(defn create-report-agent [id]
  (let [new-agent (agent [] :error-mode :continue)
        add-fn (fn[agents] 
                 (conj agents {id new-agent})
                 )]
    (swap! report-agents add-fn)
    new-agent))

(defn create-update-sender-via-agent [report-agent]
  (fn [params]
    (let [url (:patch_url params)
          fun (fn[agent-state]
                (let [res (reporter/put-as-json-with-retries url params)]
                  (conj agent-state params)))]
      (send-off report-agent fun))))


(defn send-trial-patch [report-agent full-trial-params params]
  ((create-update-sender-via-agent report-agent) 
   (conj (select-keys full-trial-params [:patch_url])
         params)))

(defn execute [params] 
  (logging/info execute params)
  (let [report-agent (create-report-agent (:domina_trial_uuid params))]
    (future 
      (try 
        (send-trial-patch report-agent params  {:started_at (util/now-as-iso8601)})
        (let [ext-prarams (git/prepare-and-create-working-dir params)
              scripts (map (fn [script-params]
                             (conj script-params (select-keys ext-prarams[:domina_execution_uuid :domina_trial_uuid :uuid 
                                                                          :working_dir :git_tree_id ])))
                           (:scripts params))]
          (logging/debug (str "processing scripts " (reduce (fn [s x] (str s " # " x)) scripts)))
          (script/process scripts (create-update-sender-via-agent report-agent))
          (send-trial-patch report-agent params  {:finished_at (util/now-as-iso8601)}))
        (catch Exception e
          (let [params (conj params {:state "failed", 
                                     :finished_at (util/now-as-iso8601)
                                     :error  (with-out-str (stacktrace/print-stack-trace e))})]
            (logging/error  (str params (with-out-str (stacktrace/print-stack-trace e))))
            ((create-update-sender-via-agent report-agent) params)))))))

