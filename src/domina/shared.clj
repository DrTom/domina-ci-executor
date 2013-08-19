; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.
 
(ns domina.shared
  (:require
    [clojure.tools.logging :as logging]
    )
  (:import 
    [java.io File]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

; TODO this should be all moved to its own ns; 
; and core should be empty / removed entirely 

(def executions (atom {}))

(def conf (atom {:report-retries 10
                 :working-dir (str (System/getProperty "user.home") (File/separator) "domina_working-dir")
                 :git-repos-dir (str (System/getProperty "user.home") (File/separator) "domina_git-repos-dir" )
                 }))

(defn initialize []
  (.mkdir (File. (:working-dir @conf)))
  (.mkdir (File. (:git-repos-dir @conf))))


