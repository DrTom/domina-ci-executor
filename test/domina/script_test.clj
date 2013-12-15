; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.script-test
  (:require  
    [clojure.pprint :as pprint]
    [domina.util :as util]
    )
  (:use 
    [clojure.test]
    [domina.script]
  ))


(deftest test-memoized-executor-exec

  (testing "a successful script" 

    (let [script-params {:name "list-env"
                         :body "env | sort"
                         :working_dir  (System/getProperty "user.home")
                         :environment_variables{
                                    :domina_trial_uuid (util/random-uuid)
                                    :domina_execution_uuid (util/random-uuid) }
                         }]

      (testing "invoking memoized-executor-exec" 
        (let [res (memoized-executor-exec  script-params)]

          (testing "the result contains the script execution result"
            (is (contains? res :error))
            (is (contains? res :exit_status))
            (is (contains? res :finished_at))
            (is (contains? res :interpreter))
            (is (contains? res :started_at))
            (is (contains? res :state))
            (is (contains? res :stderr))
            (is (contains? res :stdout)))

          (testing "the result state is success" 
            (is (= (:state res) "success")))

          ;(testing "stdout of 'env | sort' includes the defined variables"
          ;  (is (not= nil (re-find #"(?is)UUID" (:stdout res)))))

          (testing "the agent for the execution_sha1"
            (let [exec-agent  (@script-exec-agents (:domina-execution-uuid script-params))]
              (testing "is present and is an agent"
                (= (type exec-agent) clojure.lang.Agent))
              (testing "it stores the script result under its name" 
                (let [memoized-res (@exec-agent  (:name script-params))]
                  (not= nil memoized-res)
                  ))

              ))))))


  (testing "a successful script" 
    (let [script-params {:name "failing one"
                         :body "env |sort; exit -1"
                         :working_dir  (System/getProperty "user.home")
                         :environment_variables {
                                    :domina_trial_uuid (util/random-uuid)
                                    :domina_execution_uuid (util/random-uuid)}
                         }]

      (testing "invoking memoized-executor-exec" 
        (let [res (memoized-executor-exec  script-params)]
          (println (str res))
          (testing "the result contains the script execution result"
            (is (contains? res :error))
            (is (contains? res :exit_status))
            (is (contains? res :finished_at))
            (is (contains? res :interpreter))
            (is (contains? res :started_at))
            (is (contains? res :state))
            (is (contains? res :stderr))
            (is (contains? res :stdout))))
        ))))
