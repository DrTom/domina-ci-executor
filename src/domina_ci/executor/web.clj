(ns domina-ci.executor.web
  (:require 
    [compojure.core]
    [compojure.handler]
    [ring.adapter.jetty :as jetty]
    [clojure.pprint :as pprint]
    [domina-ci.executor.certificate :as certificate]
    [domina-ci.executor.execution :as execution]
    ))

(defn say-hello []
  (str "<h1>Hello!</h1>"))

(defn create-execution [request] 
  (pprint/pprint request)
  (execution/create-and-process-execution (:params request) nil)
  {:status 201
   :body(with-out-str (pprint/pprint request)) })

(compojure.core/defroutes app-routes
  (compojure.core/GET "/hello" [] (say-hello))
  (compojure.core/POST "/execute" req (create-execution req)))

(def app
  (-> (compojure.handler/site app-routes)))

(defn start-server []
  (let [keystore (certificate/create-keystore-with-certificate)
        path (.getAbsolutePath (:file keystore))
        password (:password keystore) ]
    (def server (jetty/run-jetty app {
                                      :port 8080
                                      :ssl-port 8443
                                      :ssl? true 
                                      :keystore path
                                      :key-password password
                                      :join? false}))))

(defn stop-server []
  (. server stop))
