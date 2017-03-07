(ns tools-cli.core
  (:require [think.config.core :refer [with-config get-config get-configurable-options]]
            [clojure.string :as s]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def docs
  {:setting "A setting that can be changed."})

(defn cli-options
  []
  (concat
  (for [c (get-configurable-options)]
    [nil (str "--" (name c) " "  (s/upper-case (name c))) (docs c)
     :default (get-config c)
     :id c])
  [["-h" "--help"]]))

(defn usage [options-summary]
  (->> ["Brief description."
        ""
        "Usage: program-name [options]"
        ""
        "Options:"
        options-summary]
       (s/join \newline)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn do-something
  []
  (println "setting: " (get-config :setting)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args (cli-options))]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      ;; only if your program accepts arguments...
      ;(not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (with-config (apply concat (into [] options))
      (do-something))))
