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

  (testing "creates a k/v pair in @core/executions"

    (let [uuid (.toString (java.util.UUID/randomUUID))]
      (create-and-process-execution 
        {:uuid uuid 
         :script "echo Blah"}
        nil )
      (is (not (= (@core/executions uuid) nil)))))

  (testing "evaluating something with exit 0 has state succes "
    (let [uuid (.toString (java.util.UUID/randomUUID))]
      (let [execution (create-and-process-execution 
                        {:uuid uuid 
                         :script "sleep 1;echo Hello World"}
                        nil )]
        (is (= java.lang.Thread (type (:thread execution))))
        (.join (:thread execution))
        (is (= (:state (@core/executions uuid)) "success"))
        )))
  
  (testing "the provided handler is called with the result"
    (let 
      [uuid (.toString (java.util.UUID/randomUUID))
       test-result (atom nil)
       execution (create-and-process-execution 
                   {:uuid uuid 
                    :script "echo OK"}
                   (fn [result] 
                     (swap! test-result 
                            (fn [old-result new-result] new-result) 
                            result  )))
       ]
      (.join (:thread execution))
      (is (= (:state (@core/executions uuid)) "success"))
      (is (= (type @test-result) clojure.lang.PersistentArrayMap))
      (is (= (:uuid @test-result) uuid))
      (is (not (= nil (re-matches #"(?is).*OK.*" (:stdout @test-result)))))
      ))
  )

