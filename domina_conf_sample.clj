{:shared {:report-retries 10
          ; :working-dir "/tmp/domina_working_dir"
          ; :git-repos-dir "/tmp/domina_git_repos" 
          }

 :nrepl {:port 7888
         :bind "127.0.0.1"
         :enabled false }

 :web {:host "0.0.0.0"
       :port 8088
       :ssl-port 8443}
 }
