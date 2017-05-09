(defproject thinktopic/think.config "0.3.0-SNAPSHOT"
  :description "A configuartion library."
  :url "http://github.com/thinktopic/think.config"

  :plugins [[lein-environ "1.0.0"]
            [s3-wagon-private "1.3.0"]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ "1.1.0"]
                 [org.clojure/java.classpath "0.2.3"]]

  :profiles {:test {:resource-paths ["test/resources"]
                    :env {:overwrite "80"
                          :env-config-overwrite "true"}}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "" "--no-sign"] ; disable signing
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
