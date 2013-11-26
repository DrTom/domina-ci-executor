; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.
 
(ns domina.trial
  (:import 
    [java.io File ]
    )
  (:require
    [clj-time.core :as time]
    [clojure.pprint :as pprint]
    [clojure.stacktrace :as stacktrace]
    [clojure.tools.logging :as logging]
    [domina.attachments :as attachments]
    [domina.exec :as exec]
    [domina.git :as git]
    [domina.reporter :as reporter]
    [domina.script :as script]
    [domina.util :as util]
    [domina.with :as with]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

;(set-logger! :level :debug)

(defn create-update-sender-via-agent [report-agent]
  (fn [params]
    (let [url (:patch-url params)
          fun (fn[agent-state]
                (let [res (reporter/put-as-json-with-retries url params)]
                  (conj agent-state params)))]
      (send-off report-agent fun))))


(defn send-trial-patch 
  "Sends just the patch-params" 
  [report-agent params patch-params]
  ((create-update-sender-via-agent report-agent) 
   (conj (select-keys params [:patch-url])
         patch-params)))


(defn- set-and-send-start-params [params-atom report-agent]
  (let [start-params {:started-at (time/now) :state "executing"}]
    (swap! params-atom 
           (fn [params start-params] (conj params start-params)) 
           start-params)
    (send-trial-patch report-agent @params-atom start-params)))

(defn- set-and-send-finished-params [params-atom report-agent]
  (let [finished-params {:finished-at (time/now)}]
    (swap! params-atom 
           (fn [params finished-params] (conj params finished-params)) 
           finished-params)
    (send-trial-patch report-agent @params-atom finished-params)))


(defn- prepare-scripts [params]
  (map (fn [script-params]
         (atom (conj script-params 
                     (select-keys params
                                  [:env-vars :domina-execution-uuid 
                                   :domina-trial-uuid :working-dir ]))))
       (:scripts params)))



(defonce ^:private ports-in-usage (atom #{}))

(defonce ^:private trials-atom (atom {}))

;(clojure.pprint/pprint trials-atom)
;
(defn- create-trial   
  "Creates a new trial, stores it in trials under it's id and returns the
  trial"
  [params]
  (let [id (:domina-trial-uuid params)]
    (swap! trials-atom 
           (fn [trials params id]
             (conj trials {id {:params-atom (atom params)
                               :scripts (prepare-scripts params)
                               :report-agent (agent [] :error-mode :continue)}}))
           params id)
    (@trials-atom id)))

(defn- delete-trial [id trial]
  (logging/debug "deleting trial " id)
  (swap! trials-atom (fn [trials id] #(dissoc %1 %2) id)))


; ### clean trials #######################################

(def ;once 
  ^:private trials-cleaner-stopped (atom false))

(defn start-trials-cleaner []
  (logging/info "started trials cleaner")
  (swap! trials-cleaner-stopped (fn [_] false)) 
  (future
    (loop [] 
      (with/logging-and-swallow
        (doseq [[id trial] @trials-atom] 
          (let [params @(:params-atom trial) 
                timestamp (or (:finished-at params) (:started-at params))]
            (when (> (time/in-minutes (time/interval timestamp (time/now))) 1); TODO increase
              (delete-trial id trial)))))
      (Thread/sleep (* 60 1000))
      (if-not @trials-cleaner-stopped 
        (recur)
        (logging/info "stopped trials cleaner")))))

(defn stop-trials-cleaner []
  (swap! trials-cleaner-stopped (fn [_] true)))

; ### clean trials #######################################



(defn execute [params] 
  (logging/info execute params)
  (let [working-dir (git/prepare-and-create-working-dir params)
        trial (create-trial (conj params {:working-dir working-dir}))
        params-atom (:params-atom trial)
        report-agent (:report-agent trial)]
    (future 
      (try 

        (set-and-send-start-params params-atom report-agent)

        (script/process (:scripts trial) (create-update-sender-via-agent report-agent))

        (future (attachments/put working-dir (:attachments @params-atom) (:attachments-url @params-atom)))

        (set-and-send-finished-params params-atom report-agent)

        (catch Exception e
          (swap! params-atom (fn [params] 
                               (conj params 
                                     {:state "failed", 
                                      :finished-at (time/now)
                                      :error  (with-out-str (stacktrace/print-stack-trace e))} )))
          (logging/error  (str @params-atom (with-out-str (stacktrace/print-stack-trace e))))
          ((create-update-sender-via-agent report-agent) @params-atom))))))

