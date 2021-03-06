; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.trial-test
  (:require  
    [clojure.pprint :as pprint]
    [domina.util :as util]
    [domina.trial :as trial]
    [domina.reporter :as reporter]
    [domina.git :as git]
    )
  (:use 
    [clojure.test]
    [midje.sweet]
    ))


(def default-trial-exec-params 
  {:domina_trial_id (util/random-uuid)
   :patch_url "http://localhost:8888/trial"
   })


(defn wait-for-final-state [trial]
  (deref (future 
           (loop []
             (let [state (:state @(:params-atom trial))]
               (if (or (= state "success") (= state "failed"))
                 (recur)
                 trial))))
         500 trial))

(facts "about executing a valid trial" 
       (fact "returns not failed"
             (:state @(:params-atom (wait-for-final-state (trial/execute default-trial-exec-params)))) => "success"
             (provided 
               (reporter/put-as-json-with-retries anything anything) => "" :times (range)
               (git/prepare-and-create-working-dir anything) =>  "/tmp" :times (range))
             ))

;(facts "about blah" (fact "something" (Integer/parseInt "49FREN") => (throws NumberFormatException)))

