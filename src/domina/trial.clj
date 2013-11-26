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

(defonce ^:private ports-in-usage (atom #{}))

(def ;once 
  ^:private trials-atom (atom {}))

;(clojure.pprint/pprint trials-atom)

(defn ^:private create-trial   
  "Creates a new trial, stores it in trials under it's id and returns the
  trial"
  [params]
  (let [id (:domina-trial-uuid params)]
    (swap! trials-atom 
           (fn [trials params id]
             (conj trials {id {:params-atom (atom params)
                               :report-agent (agent [] :error-mode :continue)}}))
           params id)
    (@trials-atom id)))

(defn execute [params] 
  (logging/info execute params)
  (let [working-dir (git/prepare-and-create-working-dir params)
        trial (create-trial (conj params {:working-dir working-dir}))
        params-atom (:params-atom trial)
        report-agent (:report-agent trial)]
    (future 
      (try 
        (let [start-params {:started-at (time/now) :state "executing"}]
          (swap! params-atom (fn [params] (conj params start-params)))
          (send-trial-patch report-agent @params-atom start-params))

        (let [scripts (map (fn [script-params]
                             (conj script-params 
                                   (select-keys @params-atom 
                                                [:env-vars :domina-execution-uuid 
                                                 :domina-trial-uuid :working-dir ])))
                           (:scripts @params-atom))]
          (logging/debug (str "processing scripts " (reduce (fn [s x] (str s " # " x)) scripts)))
          (script/process scripts (create-update-sender-via-agent report-agent))
          (attachments/put working-dir (:attachments @params-atom) (:attachments-url @params-atom)))

        (let [finished-params {:finished-at (time/now)}]
          (swap! params-atom (fn [params] (conj params finished-params)))
          (send-trial-patch report-agent @params-atom finished-params))

        (catch Exception e
          (swap! params-atom (fn [params] 
                               (conj params 
                                     {:state "failed", 
                                      :finished-at (util/now-as-iso8601)
                                      :error  (with-out-str (stacktrace/print-stack-trace e))} )))
          (logging/error  (str @params-atom (with-out-str (stacktrace/print-stack-trace e))))
          ((create-update-sender-via-agent report-agent) @params-atom))))))


