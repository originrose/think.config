(defproject thinktopic/think.config "0.2.2"
  :description "A configuartion library."
  :url "http://github.com/thinktopic/think.config"

  :plugins [[lein-environ "1.0.0"]
            [codox "0.8.12"]
            [s3-wagon-private "1.1.2"]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [environ "1.1.0"]
                 [org.clojure/java.classpath "0.2.3"]]

  :profiles {:dev {:repl-options {:init-ns user}
                   :source-paths ["dev"]}
             :test {:resource-paths ["test/resources"]
                    :env {:overwrite "80"
                          :env-config-overwrite "true"}}
             :uberjar {:aot :all}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "" "--no-sign"] ; disable signing
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :repositories  {"snapshots"  {:url "s3p://thinktopic.jars/snapshots/"
                                :passphrase :env
                                :username :env
                                :releases false
                                :sign-releases false}
                  "releases"  {:url "s3p://thinktopic.jars/releases/"
                               :passphrase :env
                               :username :env
                               :snapshots false
                               :sign-releases false}})
