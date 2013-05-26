(ns domina-ci.executor.web
  (:require 
    [compojure.core]
    [compojure.handler]
    [ring.adapter.jetty :as jetty]
    [clojure.pprint :as pprint]
    [domina-ci.executor.certificate :as certificate]
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
  (let [keystore (certificate/create-keystore-with-certificate)]
    (def server (jetty/run-jetty app {
                                      :port 8080
                                      :ssl-port 8443
                                      :ssl? true 
                                      :keystore (.getAbsolutePath (:file keystore))
                                      :key-password (:password keystore)
                                      :join? false}))))

(defn stop-server []
  (. server stop))
