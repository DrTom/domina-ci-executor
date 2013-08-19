; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.report
  (:use 
    [clojure.stacktrace :only (print-stack-trace)])
  (:require 
    [clj-http.client :as http-client]
    [clj-time.core :as time]
    [clj-time.format :as time-format]
    [clojure.data.json :as json]
    [clojure.tools.logging :as logging]
    [domina.shared :as shared]
    ))

(defn send-as-json [url params retries-counter]
  (Thread/sleep (* retries-counter retries-counter 1000))
  (try
    (logging/info (str "SENDING " (select-keys params [:name :state :exit_status]) url ))
    (http-client/put
      url
      {:insecure? true
       :content-type :json
       :accept :json 
       :body (json/write-str params)})
    (catch Exception e 
      (logging/error  (str "reporting a script-execution-result failed" params (with-out-str (print-stack-trace e))))
      (if (< retries-counter (:report-retries @shared/conf))
        (future (send-as-json url params (inc retries-counter)))))))

