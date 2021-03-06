(ns domina.with
  (:require 
    [domina.util :as util]
    [clj-logging-config.log4j :as logging-config]
    [clojure.stacktrace :as stacktrace]
    [clojure.tools.logging :as clj-logging]
    ))

;(logging-config/set-logger! :level :debug)

(defmacro logging [& expressions]
  `(try
     ~@expressions
     (catch Throwable e#
       (clj-logging/error (util/application-trace e#))
       (throw e#))))


;(macroexpand '(logging (/ 1 0)))

(defmacro logging-and-swallow [& expressions]
  `(try
     ~@expressions
     (catch Throwable e#
       (clj-logging/error (util/application-trace e#))
       nil)))

(defmacro catch-and-fail-state [_atom & body]
  "blah"
  `(try
     ~@body
     ~_atom
     (catch Exception e#
       (let [error# (with-out-str (stacktrace/print-throwable e#))
             trace#  (util/application-trace e#)]
         (clj-logging/error e# trace#)
         (swap! ~_atom
                (fn [curr# er# tr#]
                  (conj curr# {:state "failed", :error er#, :trace tr#}))
                error# trace#)
         )
       ~_atom
       )
     ))

;(macroexpand '(catch-and-fail-state x (+ @x 1)))


