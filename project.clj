(defproject thinktopic/think.config "0.1.0-SNAPSHOT"
  :description "A configuartion library."
  :url "http://github.com/thinktopic/think.config"

  :plugins [[lein-environ "1.0.0"]
            [codox "0.8.12"]
            [s3-wagon-private "1.1.2"]]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [environ "1.0.0"]
                 [org.clojure/java.classpath "0.2.2"]
                 [com.taoensso/nippy "2.11.1"]
                 [clojurewerkz/propertied "1.2.0"]
                 [com.novemberain/langohr "3.5.1"]]

  :profiles {:dev {:repl-options {:init-ns user}
                   :dependencies [[thinktopic/vault-clj "0.2.1"]]
                   :source-paths ["dev"]}
             :uberjar {:aot :all}}

  :repositories  {"snapshots"  {:url "s3p://thinktopic.jars/snapshots/"
                                :passphrase :env
                                :username :env
                                :releases false
                                :sign-releases false}
                  "releases"  {:url "s3p://thinktopic.jars/releases/"
                               :passphrase :env
                               :username :env
                               :snapshots false
                               :sign-releases false}}
  )
