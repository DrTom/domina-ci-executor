; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.git
  (:import 
    [java.io File]
    )
  (:require 
    [clojure.java.shell :as shell]
    [clojure.pprint :as pprint]
    [clojure.string :as string]
    [clojure.tools.logging :as logging]
    [domina.shared :as shared]
    )
  (:use 
    [clj-logging-config.log4j :only (set-logger!)]
    ))

;(set-logger! :level :info)

(defn canonical-repository-path [prepository-id]
  (str (:git-repos-dir @shared/conf) (File/separator) prepository-id))

(defn create-mirror-clone [url path] 
  (logging/debug (str "create-mirror-clone " url " " path ))
  (let [ res (shell/sh "git" "clone" "--mirror" url path)]
    (if (not= 0 (:exit res)) 
      (logging/error (str (:out res) (:err res)))
      (throw (Exception. (:err res)))
      )))

(defn repository-includes-commit? [path commit-id]
  "Returns false if there is an exeception!" 
  (logging/debug (str "repository-includes-commit?"))
  (let [res (shell/sh "git" "log" "-n" "1" "--format='%H'" commit-id 
                      :dir path)] 
    (= 0 (:exit res))))

(defn update [path]
  (logging/debug (str "update " path))
  (let [res (shell/sh "git" "remote" "update" :dir path)]
    (if (not= 0 (:exit res)) (throw (Exception. (:err res))))))



; use an agent to initialize and update a repo so we don't have
; multiple threads going about it

(def repositories (atom {}))

(defn repository-agent [repository-id]
  (@repositories repository-id))

(defn create-repository-agent [repository-url repository-id]
  (swap! repositories (fn [repositories repository-url repository-id ]
                        (conj repositories {repository-id (agent {:commit-ids #{} 
                                                                  :repository-id repository-id
                                                                  :repository-url repository-url
                                                                  :repository-path (canonical-repository-path repository-id) } 
                                                                 :error-mode :continue)})
                        )repository-url repository-id)
  (repository-agent repository-id))


(defn initialize-or-update-if-required [agent-state repository-url repository-id commit-id]
  (logging/debug " initialize-or-update-if-required" agent-state repository-id repository-id commit-id)
  (let [repository-path (:repository-path agent-state)]
    (if-not (.isDirectory (File. repository-path)) 
      (create-mirror-clone repository-url repository-path))
    (if-not (repository-includes-commit? repository-path commit-id)
      (update repository-path))
    (conj agent-state {:commit-ids (conj (:commit-ids agent-state) commit-id)})))

(defn serialized-initialize-or-update-if-required [repository-url repository-id commit-id]
  (logging/debug " serialized-initialize-or-update-if-required " repository-url repository-id commit-id)
  (if-not (and repository-url repository-id commit-id)
    (throw (java.lang.IllegalArgumentException. "serialized-initialize-or-update-if-required")))
  (let [repository-agent (or (repository-agent repository-id)
                             (create-repository-agent repository-url repository-id))
        state @repository-agent]
    (logging/debug repository-agent)
    (if-not ((:commit-ids state) commit-id)
      (do 
        (logging/debug (str "sending-off and await initialize-or-update-if-required "))
        (send-off (@repositories repository-id) initialize-or-update-if-required 
                  repository-url repository-id commit-id)
        (await repository-agent)))
    (:repository-path @repository-agent)))


(defn clone-to-working-dir [repository-path commit-id working-dir]
  (logging/debug "clone-to-working-dir " repository-path " " commit-id " " working-dir)
  (logging/debug " git " " clone " " --shared " repository-path working-dir)
  (let [res (shell/sh "git" "clone" "--shared" repository-path working-dir)]
    (if (not= 0 (:exit res)) (throw (Exception. (:err res))))
    (let [res (shell/sh "git" "checkout" commit-id :dir working-dir)]
      (if (not= 0 (:exit res)) 
        (throw (Exception. (:err res)))
        true))))

(defn prepare-and-create-working-dir [params]
  (logging/debug "prepare-and-create-working-dir" str params)
  (let [repository-path (serialized-initialize-or-update-if-required 
                          (:git-url params) (:git-repository-id params) (:git-commit-id params))
        working-dir (str (:working-dir @shared/conf) (File/separator) (:uuid params)) ]
    (clone-to-working-dir repository-path (:git-commit-id params) working-dir)
    (logging/debug "WORKING-DIR " working-dir)
    (conj params {:working-dir working-dir})
    ))


; FOR PROTOTYPING
; (create-repository-agent "http://localhost:3013/repositories/PrototypeRepo/git" "TestX")
; (def repo-path (serialized-initialize-or-update-if-required "http://localhost:3013/repositories/PrototypeRepo/git" "TestX" "e4e1e98"))


