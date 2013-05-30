(ns domina-ci.executor.core)

(def executions (atom {}))

(def conf (atom { :report-retries 100
                 }))
