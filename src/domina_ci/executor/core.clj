(ns domina-ci.executor.core)

(def executions (atom {}))

(def conf (atom {:report-retries 100
                 :working-dir "/tmp/domina_ci/working_dir"
                 :git-repositories "/tmp/domina_ci/repositories"
                 }))
