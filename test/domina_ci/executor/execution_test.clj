(ns domina-ci.executor.execution-test
  (:require  
    [clojure.pprint :as pprint]
    [domina-ci.executor.core :as core])
  (:use 
    [clojure.test]
    [domina-ci.executor.execution]
  ))


(deftest test-swap-in-execution-params 
  (let [uuid (.toString (java.util.UUID/randomUUID))]
    (swap-in-execution-params uuid {:x 42})
    (is (= (@core/executions uuid) {:x 42}))))

(deftest test-create-and-process-execution

  (let [uuid (.toString (java.util.UUID/randomUUID))]
    (testing "creates a k/v pair in @core/executions"
      (create-and-process-execution 
        {:uuid uuid 
         :script "echo Blah"}
        nil )
      (is (not (= (@core/executions uuid) nil)))))

  (let [uuid (.toString (java.util.UUID/randomUUID))]
    (testing "evaluating something with exit 0 has state succes "
      (let [execution (create-and-process-execution 
                        {:uuid uuid 
                         :script "sleep 1;echo Hello World"}
                        nil )]
        (is (= java.lang.Thread (type (:thread execution))))
        (.join (:thread execution))
        (is (= (:state (@core/executions uuid)) "success"))
        ))
    ))

