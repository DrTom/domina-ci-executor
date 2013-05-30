(ns domina-ci.executor.report
  (:use 
    [clojure.stacktrace :only (print-stack-trace)])
  (:require 
    [clj-http.client :as http-client]
    [clj-time.core :as time]
    [clj-time.format :as time-format]
    [clojure.tools.logging :as logging]
    [domina-ci.executor.core :as core]
    ))

(defn send-execution-update-to-server 
  ([params] (send-execution-update-to-server params 0)) 
  ([params count]
   (logging/info "updating task")
   (let [data (select-keys params [:state :stdout :stderr])]
     (try
       (logging/info (str data))
       ; TODO see if and how we can use patch
       (http-client/put
         (:patch_url params)
         {:insecure? true
          :content-type :json
          :accept :json 
          :form-params data})
       (catch Exception e 
         (if (< count (:report-retries @core/conf))
           (do (logging/warn (str "reporting a task failed, trying again soon; " e))
               (. Thread sleep (* 60 1000))
               (send-execution-update-to-server params (inc count)))
           (logging/error  
             (str "reporting a task failed finally;  "
                  params
                  (with-out-str (print-stack-trace e))))))))))
