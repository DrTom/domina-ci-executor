; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.
 
(ns domina.trial
  (:import 
    [java.io File ]
    )
  (:require
    [clojure.pprint :as pprint]
    [clojure.stacktrace :as stacktrace]
    [clojure.tools.logging :as logging]
    [domina.exec :as exec]
    [domina.git :as git]
    [domina.reporter :as reporter]
    [domina.attachments :as attachments]
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


(defn send-trial-patch [report-agent full-trial-params params]
  ((create-update-sender-via-agent report-agent) 
   (conj (select-keys full-trial-params [:patch-url])
         params)))

; 

(defonce ^:private ports-in-usage (atom #{}))

(defonce ^:private trials-atom (atom {}))

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

; TODO
; test it, it should run
; update the params in place 

(defn execute [params] 
  (logging/info execute params)
  (let [ext-prarams (git/prepare-and-create-working-dir params)
        trial (create-trial ext-prarams)      
        report-agent (:report-agent trial)]
    (future 
      (try 
        (send-trial-patch report-agent params  {:started-at (util/now-as-iso8601)})
        (let [scripts (map (fn [script-params]
                             (conj script-params (select-keys ext-prarams [:env-vars :domina-execution-uuid 
                                                                           :domina-trial-uuid :working-dir ])))
                           (:scripts params))]
          (logging/debug (str "processing scripts " (reduce (fn [s x] (str s " # " x)) scripts)))
          (script/process scripts (create-update-sender-via-agent report-agent))
          (attachments/put (:working-dir ext-prarams) (:attachments ext-prarams) (:attachments-url ext-prarams))
          (send-trial-patch report-agent params  {:finished-at (util/now-as-iso8601)}))
        (catch Exception e
          (let [params (conj params {:state "failed", 
                                     :finished-at (util/now-as-iso8601)
                                     :error  (with-out-str (stacktrace/print-stack-trace e))})]
            (logging/error  (str params (with-out-str (stacktrace/print-stack-trace e))))
            ((create-update-sender-via-agent report-agent) params)))))))

