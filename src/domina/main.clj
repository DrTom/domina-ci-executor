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
    [domina.nrepl :as nrepl]
    [domina.reporter :as reporter]
    [domina.shared :as shared]
    [domina.trial :as trial]
    [domina.util :as util]
    [domina.web :as web]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

;(set-logger! :level :debug)

(defn read-config []
  (logging/info "read-config invoked")
  (util/try-read-and-apply-config 
    {:shared shared/conf 
     :nrepl nrepl/conf
     :reporter reporter/conf
     :web web/conf} 
    "/etc/domina/conf"
    "/etc/domina_conf"
    (str (System/getProperty "user.home") (File/separator) "domina_conf")
    "domina_conf"))

(defn -main
  [& args]
  (logging/info "starting -main " args)
  (read-config)
  (shared/initialize)
  (trial/initialize)
  (nrepl/start-server)
  (web/start-server))

