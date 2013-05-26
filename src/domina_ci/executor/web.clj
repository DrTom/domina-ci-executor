(ns domina-ci.executor.web
  (:require 
    [compojure.core]
    [compojure.handler]
    [ring.adapter.jetty :as jetty]
    [clojure.pprint :as pprint]
    ))

(defn say-hello []
  (str "<h1>Hello!</h1>"))

(compojure.core/defroutes app-routes
  (compojure.core/GET "/hello" [] (say-hello))
  (compojure.core/POST "/execute/" [] ())
  )

(def app
  (-> (compojure.handler/site app-routes)))

(defn start-server []
  (def server (jetty/run-jetty app {:port 8080,:ssl? true, :join? false})))

(defn stop-server []
  (. server stop))
