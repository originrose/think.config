(defproject tools-cli "0.1.0-SNAPSHOT"
  :description "An example of using think.config for tools.cli"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [thinktopic/think.config "0.2.2"]
                 [org.clojure/tools.cli "0.3.5"]]

  :plugins [[s3-wagon-private "1.1.2"]]

  :main tools-cli.core

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
