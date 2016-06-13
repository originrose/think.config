(ns think.config.core
  "A Config namespace for deriving app config.
  This will pull from your environment or lein"
  (:require [environ.core :refer [env]]
            [clojure.java.classpath :as cp]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [clojure.pprint :as pprint]
            [clojure.java.shell :refer [sh]] ))

(def config-sources* (atom {}))

(defn input-stream-to-map
  [stream]
  (with-open [s stream]
    (edn/read (java.io.PushbackReader. s))))

(defn is-config-file?
  [file]
  (.contains (.getName file) "config.edn"))

(defn get-config-files
  []
  (->> (cp/classpath-directories)
       (map file-seq)
       (flatten)
       (filter is-config-file?)
       (mapv (fn [f] [(.getName f) (io/reader f)]))))

(defn flatten-config-map [m]
  (apply concat (for [[k v] m]
                  (map (fn [val] [k val])
                       v))))

(defn get-config-streams [jar-files]
  (let [jar-file-configs (map (fn [jarfile]
                                [jarfile
                                 (filter is-config-file?
                                         (enumeration-seq (.entries jarfile)))])
                              jar-files)
        jar-config-map (filter #(> (count (second %)) 0) jar-file-configs)
        flattened-config-map (flatten-config-map jar-config-map)]
    (map (fn [[jarfile jarentry]]
           [(str (.getName jarfile) "/" (.getName jarentry))
            (io/reader (.getInputStream jarfile jarentry))])
         flattened-config-map)))

(defn get-config-edn-values
  []
  (let [config-streams (concat (get-config-streams (cp/classpath-jarfiles))
                               (get-config-files))
        config-stream-names (mapv first config-streams)
        short-name-fn (partial re-find #"[^\/]+$")
        short-names (reverse (sort (mapv short-name-fn config-stream-names)))
        sorted-config-streams (reverse (sort-by (comp short-name-fn first) config-streams))
        config-maps (map (comp input-stream-to-map second) sorted-config-streams)]
    (doall
     (map (fn [n m]
            (doseq [[k _] m]
              (swap! config-sources* assoc k n)))
          short-names config-maps))
    (log/info "Found config files:")
    (doseq [stream-name (sort config-stream-names)]
      (log/info (str "  " stream-name)))
    (reduce merge {} config-maps)))

(defn print-map-as-table
  [m]
  (let [table (map (fn [[k v]]
                     {:key k
                      :value (str v)
                      :source (get @config-sources* k)})
                   m)
        key-width (min 30 (apply max (map (comp count str :key) table)))
        val-width (min 30 (apply max (map (comp count str :value) table)))
        src-width (min 20 (apply max (map (comp count str :source) table)))]
    (log/info "Config values at startup:")
    (with-bindings {#'pprint/*print-right-margin* nil}
      (log/info (format (str "%-" key-width "s %-" val-width "s %-" src-width "s") "Key" "Value" "Source"))
      (log/info (apply str (repeat (+ key-width val-width src-width 2) "-")))
      (doseq [{:keys [key value source]} table]
        (log/info (format (str "%-" key-width "s %-" val-width "s %-" src-width "s")
                         key value source))))))

(defn build-config-env
  []
  (let [config-map (get-config-edn-values)
        final-map (merge config-map env)
        ;;disabling these printfs as we have decided that dumping the env
        ;;is a security risk
        ;;_ (println "config map\n" config-map)
        ;;_ (println "env\n" env)
        ;;Only print out keys found in the config files.  Do not
        ;;print out entire environment
        print-map (into {} (filter #(contains? config-map (% 0)) final-map))]
    (doseq [k (clojure.set/intersection (set (keys env))
                                        (set (keys print-map)))]
      (swap! config-sources* assoc k "environment"))
    (print-map-as-table (into (sorted-map) print-map))
    final-map))

(def ^:dynamic *config-map* nil)

(defn init-config-map []
  (alter-var-root (var *config-map*) (fn [_] (build-config-env))))

(defn get-config-map
  []
  (when-not *config-map*
    (init-config-map))
  *config-map*)

(defn unchecked-get-config
  [k]
  ((get-config-map) k))

(defn get-config
  "Get App Config. Accepts a key such as \"PORT\" or :port."
  ([k]
  (let [retval (unchecked-get-config k)]
    (when (nil? retval)
      (throw (IllegalArgumentException. (format "Missing config value: %s" k))))
    retval))
  ([k read-string?]
    (if (and (string? (get-config k)) read-string?)
      (read-string (get-config k))
      (get-config k))))

(defn set-config!
  "Very dangerous, but useful during testing.  Set a config value"
  [key value]
  (let [old-map (get-config-map)]
    (alter-var-root (var *config-map*) assoc key value)
    (old-map key)))


(defmacro with-config
  [config-key-vals & body]
  `(let [new-map# (apply assoc (get-config-map) ~config-key-vals)]
     (with-bindings {#'*config-map* new-map#}
       ~@body)))


;; (defn get-ip-address
;;   [& {:keys [public-addr]}]
;;   (if-let [ip-addr (unchecked-get-config :ip-address)]
;;     ip-addr
;;     (if (unchecked-get-config :aws-instance)
;;       (if public-addr
;;         (slurp "http://169.254.169.254/latest/meta-data/public-ipv4")
;;         (slurp "http://169.254.169.254/latest/meta-data/local-ipv4"))
;;       (.getHostAddress (java.net.Inet4Address/getLocalHost)))))
