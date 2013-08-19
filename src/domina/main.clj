; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.
;
(ns domina.main
  (:gen-class)
  (:import 
    [java.io File]
    )
  (:require 
    [clojure.tools.logging :as logging]
    [domina.shared :as shared]
    [domina.nrepl :as nrepl]
    [domina.util :as util]
    [domina.web :as web]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

(set-logger! :level :info)

(defn read-config []
  (logging/debug "read-config invoked")
  (util/try-read-and-apply-config 
    {:shared shared/conf 
     :nrepl nrepl/conf
     :web web/conf} 
    "/etc/domina/conf.clj"
    "/etc/domina_conf.clj"
    (str (System/getProperty "user.home") (File/separator) "domina_conf.clj")
    "domina_conf.clj"))

(defn -main
  [& args]
  (logging/info "starting -main " args)
  (read-config)
  (shared/initialize)
  (nrepl/start-server)
  (web/start-server))

