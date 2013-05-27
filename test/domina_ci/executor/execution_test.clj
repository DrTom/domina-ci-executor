(ns domina-ci.executor.execution-test
  (:require  
    [domina-ci.executor.core :as core])
  (:use 
    [clojure.test]
    [domina-ci.executor.execution]
  ))


(deftest test-create-and-process-execution

  (let [uuid "76b76133-e45c-427a-afe6-9f5dfd165228"]

    (testing "creates a k/v pair in @core/executions"
      (create-and-process-execution 
        {:uuid uuid} 
        nil )
      (is (not (= (@core/executions uuid) nil))))
    
    (testing "evaluating something with exit 0 has state succes "
      (create-and-process-execution 
        {:uuid uuid 
         :command "ls -lah"}
        #() )
      (is (= (:state (@core/executions uuid)) "success")))
    
    ))

