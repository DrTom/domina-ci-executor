(ns domina-ci.executor.execution-test
  (:require  
    [domina-ci.executor.core :as core])
  (:use 
    [clojure.test]
    [domina-ci.executor.execution]
  ))


(deftest test-swap-in-execution-params 
  (let [uuid "b12ce8a1-f3e8-4e13-acee-e40c9ef9e67c"]
    (swap-in-execution-params uuid {:x 42})
    (is (= (@core/executions uuid) {:x 42}))))

;(deftest test-create-and-process-execution
;
;  (let [uuid "76b76133-e45c-427a-afe6-9f5dfd165228"]
;
;    (testing "creates a k/v pair in @core/executions"
;      (create-and-process-execution 
;        {:uuid uuid} 
;        nil )
;      (is (not (= (@core/executions uuid) nil))))
;
;    (testing "evaluating something with exit 0 has state succes "
;      (let [execution (create-and-process-execution 
;                      {:uuid uuid 
;                       :script "ls -lah"}
;                      #() )]
;        (.join (:thread execution))
;        (is (= (:state (@core/executions uuid)) "success")))
;
;      )))

