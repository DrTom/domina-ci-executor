(ns domina-ci.executor.execution-test
  (:require  
    [clojure.pprint :as pprint]
    [domina-ci.executor.core :as core])
  (:use 
    [clojure.test]
    [domina-ci.executor.execution]
  ))


(deftest test-swap-in-execution-params 
  (let [uuid "b12ce8a1-f3e8-4e13-acee-e40c9ef9e67c"]
    (swap-in-execution-params uuid {:x 42})
    (is (= (@core/executions uuid) {:x 42}))))

(deftest test-create-and-process-execution

  (let [uuid "92190cd7-7195-45ce-8520-fc65cb6d4e5a"]
    (testing "creates a k/v pair in @core/executions"
      (create-and-process-execution 
        {:uuid uuid 
         :script "echo Blah"}
        nil )
      (is (not (= (@core/executions uuid) nil)))))

  (let [uuid "76b76133-e45c-427a-afe6-9f5dfd165228"]
    (testing "evaluating something with exit 0 has state succes "
      (let [execution (create-and-process-execution 
                        {:uuid uuid 
                         :script "echo Hello World"}
                        #(pprint/pprint %) )]
        (pprint/pprint execution)
        (is (= java.lang.Thread (type (:thread execution))))
        (.join (:thread execution))
        (is (= (:state (@core/executions uuid)) "success"))
        ))
    ))

