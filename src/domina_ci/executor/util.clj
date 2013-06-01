(ns domina-ci.executor.util
  (:require 
    [clj-time.core :as time]
    [clj-time.format :as time-format]
    [clojure.string :as string]
    [clojure.pprint :as pprint]
    ))

(defn date-time-to-iso8601 [date-time]
  (time-format/unparse (time-format/formatters :date-time) date-time))


(defn split-in-dirs [s]
  (let [dirs-seq1  (string/split s #"/")
        dirs-seq2 (filter #(not (string/blank? %)) dirs-seq1)
        ]
    dirs-seq2
    ))



