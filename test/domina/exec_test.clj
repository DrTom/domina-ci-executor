; Copyright (C) 2013 Dr. Thomas Schank 
; Licensed under the terms of the GNU Affero General Public License v3.
; See the "LICENSE.txt" file provided with this software.

(ns domina.exec-test
  (:require  
    [clojure.pprint :as pprint]
    [domina.util :as util]
    )
  (:use 
    [clojure.test]
    [domina.exec]
  ))


(deftest test-exec-script

  (testing "invoking exec-script succeeds and returns expected parameters" 
    (let [res (exec-script "ls")]
      (is (contains? res :out ))
      (is (not= (:exit res) nil))
      (is (not= (:err res) nil)) 
      (is (= (:exit res) 0))
      ))

  (testing "invoking exec-script with timeout" 
    (let [res (exec-script "sleep 2; ls" :timeout 1)]
      (println res)
      (is (= (:exit res) -1))
      )))

(deftest test-exec-script-for-params
  (let [def-params {:body "env | sort"
                    :working-dir  (System/getProperty "user.home")
                    :uuid (util/random-uuid)
                    :domina_trial_uuid (util/random-uuid)
                    :domina_execution_uuid (util/random-uuid)
                    }]
    (testing "invoking exec-script-for-params" 
      (let [res (exec-script-for-params def-params)]
        (is (contains? res :error))
        (is (contains? res :exit_status))
        (is (contains? res :finished_at))
        (is (contains? res :interpreter_command))
        (is (contains? res :started_at))
        (is (contains? res :state))
        (is (contains? res :stderr))
        (is (contains? res :stdout))
        (testing "stdout of 'env | sort' includes the defined variables"
          (is (not= nil (re-find #"(?is)UUID" (:stdout res)))))
        ))))

