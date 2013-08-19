; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.web
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    [clojure.stacktrace :only (print-stack-trace)]
    )
  (:require 
    [clj-time.core :as time]
    [clojure.pprint :as pprint]
    [clojure.tools.logging :as logging]
    [clojure.data :as data]
    [compojure.core]
    [compojure.handler]
    [domina.certificate :as certificate]
    [domina.exec :as exec]
    [domina.git :as git]
    [domina.report :as report]
    [domina.util :as util]
    [domina.script :as script]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.json]
    ))

(set-logger! :level :info)

(defonce conf (atom {:host "0.0.0.0"
                     :port 8088
                     :ssl-port 8443}))

(defn say-hello []
  (str "<h1>Hello!</h1>"))

(defn ping [] 
  (logging/debug "pinging back")
  {:status 204}
  )

(defn send-script-exec-result [script-exec-result]
  (report/send-as-json (:patch_url script-exec-result) script-exec-result 0))

(defn execute [request] 
  (logging/info (str "received execution request: " request))
  (future 
    (try 
      (let [params  (clojure.walk/keywordize-keys (:json-params request))
            ext-prarams (git/prepare-and-create-working-dir (:params request))
            scripts (map (fn [script-params]
                           (conj script-params (select-keys ext-prarams[:domina_execution_uuid :domina_trial_uuid :uuid 
                                                                        :working_dir :git_tree_id, :trial_id, :execution_sha1, 
                                                                        :execution_id])))
                         (:scripts params))]
        (logging/debug (str "processing scripts " (reduce (fn [s x] (str s " # " x)) scripts)))
        (script/process scripts send-script-exec-result))
      (catch Exception e
        (let [params (conj (:params request) {:state "failed", :error  (with-out-str (print-stack-trace e))})]
          (logging/error  (str params (with-out-str (print-stack-trace e))))
          (report/send-as-json (:patch_url params) params 0)))))
  {:status 201
   :body (with-out-str (pprint/pprint request))})

(compojure.core/defroutes app-routes
  (compojure.core/GET "/hello" [] (say-hello))
  (compojure.core/POST "/ping" [] (ping))
  (compojure.core/POST "/execute" req (execute req)))

(def app
  ( -> (compojure.handler/site app-routes)
       (ring.middleware.json/wrap-json-params)))


(defonce server nil)

(defn stop-server []
  (logging/info "stopping server")
  (. server stop)
  (def server nil))


(defn start-server []
  "Starts (or stops and then starts) the webserver"
  (let [keystore (certificate/create-keystore-with-certificate)
        path (.getAbsolutePath (:file keystore))
        password (:password keystore) 
        server-conf (conj {:ssl? true 
                           :keystore path
                           :key-password password
                           :join? false} @conf)]
    (if server (stop-server)) 
    (logging/info "starting server" server-conf)
    (def server (jetty/run-jetty app server-conf))))
