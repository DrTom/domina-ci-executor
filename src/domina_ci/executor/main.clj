(ns domina-ci.executor.main
  (:gen-class)
  (:require [domina-ci.executor.web :as web]))


(defn -main
  [& args]
  (web/start-server))
