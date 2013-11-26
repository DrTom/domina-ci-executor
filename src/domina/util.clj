; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.util
  (:import
    [java.util UUID]
    )
  (:require 
    [clj-time.core :as time]
    [clj-time.format :as time-format]
    [clojure.pprint :as pprint]
    [clojure.string :as string]
    [clojure.stacktrace :as stacktrace]
    [clojure.tools.logging :as logging]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

;(set-logger! :level :debug)

(defn filter-trace [tr regex]
  (concat [(with-out-str (stacktrace/print-throwable tr))]
          (filter (fn [l] (re-matches regex l))
                  (map (fn [e] (with-out-str (stacktrace/print-trace-element e)))
                       (.getStackTrace tr)
                       ))))


(defn application-trace [tr]
  (filter-trace tr #".*domina.*"))


(defn uuid-to-short [uuid]
  (let [juuid (UUID/fromString uuid)
        least-sig-bytes (.getLeastSignificantBits juuid) ]
    (-> least-sig-bytes .intValue .shortValue (- Short/MIN_VALUE))
    ))

(defn date-time-to-iso8601 [date-time]
  (time-format/unparse (time-format/formatters :date-time) date-time))

(defn now-as-iso8601 [] (date-time-to-iso8601 (time/now)))

(defn split-in-dirs [s]
  (let [dirs-seq1  (string/split s #"/")
        dirs-seq2 (filter #(not (string/blank? %)) dirs-seq1)
        ]
    dirs-seq2
    ))


; rather use key-fn option of json/write-str
(defn upper-case-keys [some-hash]
  (into {} (map (fn [p] [ (string/upper-case (name (first p))) (second p)] ) some-hash)))

(defn rubyize-keys [some-hash]
  (into {} (map (fn [p] [(keyword (string/replace (name (first p)) #"-" "_")) (second p)] ) some-hash)))

(defn hash-to-env-opts [h]
  (map #(str (string/upper-case (name (first %1))) "=" (last %1)) h))

(defn random-uuid []
  (.toString (java.util.UUID/randomUUID)))

(defn ammend-config [conf_atom data]
  (logging/info "amending config " conf_atom " with " data)
  (swap! conf_atom 
         (fn [current more] 
           (conj current more))
         data))

(defn try-read-and-apply-config [configs & filenames]
  (doseq [filename filenames]
    (try 
      (let [file-config (read-string(slurp filename))]
        (logging/info "successfully read " filename " with content: " file-config)
        (doseq [[k config] configs]
          (if-let [config-section (k file-config)]
            (do 
              (logging/info "amending config " k)
              (ammend-config
                config
                config-section)))))
      (catch Exception e (do (logging/info (str "could not read " filename " " e))))
      )))
