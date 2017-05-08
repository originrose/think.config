(ns think.config.core
  "A Config namespace for deriving app config.  This will pull from your
  environment or lein"
  (:require [environ.core :refer [env]]
            [clojure.java.classpath :as cp]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(def ^:dynamic *config-sources* {})
(def ^:dynamic *config-map* nil)

(defn- input-stream-to-map
  "Helper function used when reading configs from jars in the class-path. Takes
  an input stream and returns a map for the data contained in that input
  stream."
  [stream]
  (with-open [^java.io.BufferedReader s stream]
    (edn/read (java.io.PushbackReader. s))))

(defn- is-config-file?
  [^java.io.File file]
  (.contains (.getName file) "config.edn"))

(defn- is-config-entry?
  [^java.util.jar.JarEntry entry]
  (.contains (.getName entry) "config.edn"))

(defn- coercing-merge
  "Takes two maps and merges the source into the dest while trying to coerce
  values into the type of the destination map. This is so that a base config
  (e.g. in a library) can specify types and they can be overwritten by strings
  from the outside (e.g. either via the command line or the environment) and
  have those values become the correct type."
  [dest src]
  (reduce (fn [dest-map [k v]]
            (if-not (contains? dest-map k)
              (assoc dest-map k v)
              (let [d (get dest-map k)]
                (cond
                  (nil? d) (assoc dest-map k v)
                  (string? d) (assoc dest-map k (str v))
                  (float? d) (assoc dest-map k (Double. (str v)))
                  (integer? d) (assoc dest-map k (Integer. (str v)))
                  (keyword? d) (assoc dest-map k
                                      (if (and (string? v) (not-empty v) (= \: (first v)))
                                        (keyword (subs v 1))
                                        (keyword v)))
                  (instance? Boolean d) (assoc dest-map k (boolean (Boolean. (str v))))
                  :default (assoc dest-map k (edn/read-string  v))))))
          dest
          src))

(defn- get-config-streams
  "Returns BufferedReaders for *-config.edn files found in jar-files."
  [jar-files]
  (->> jar-files
       (map (fn [^java.util.jar.JarFile jarfile]
              [jarfile
               (filter is-config-entry?
                       (enumeration-seq (.entries jarfile)))]))
       (filter #(> (count (second %)) 0))
       ;; Flatten the config map (make sure that all of the values map to their source)
       ((fn [m]
          (for [[k v] m
                val_ v]
            [k val_])))
       (mapv (fn [[^java.util.jar.JarFile jarfile ^java.util.jar.JarEntry jarentry]]
           [(str (.getName jarfile) "/" (.getName jarentry))
            (io/reader (.getInputStream jarfile jarentry))]))))

(defn- get-config-edn-values
  "Loops through all of the .edn files in the jars as well as resources and
  coerce-merges them reverse alphabetically with app-config and user-config
  taking precedence over the remaining, respectively."
  []
  (let [short-name-fn (partial re-find #"[^\/]+$")
        move-to-end-fn (fn [entry coll]
                         (let [m (group-by #(= (short-name-fn (first %)) entry) coll)]
                           (concat (get m false) (get m true))))]
    (->> (cp/classpath-directories)
         (map file-seq)
         (flatten)
         (filter is-config-file?)
         (map (fn [^java.io.File f] [(.getName f) (io/reader f)]))
         (concat (get-config-streams (cp/classpath-jarfiles)))
         (sort-by (comp short-name-fn first))
         (move-to-end-fn "app-config.edn")
         (move-to-end-fn "user-config.edn")
         (reduce (fn [eax [path file]]
                   (let [m (input-stream-to-map file)]
                     (doseq [[k _] m]
                       (when-not (contains? *config-sources* k)
                         (alter-var-root (var *config-sources*) #(assoc % k (short-name-fn path)))))
                     (coercing-merge eax m))) {}))))

(defn- build-config-env
  "Squashes the environment onto the config-*.edn files."
  []
  (let [config-map (get-config-edn-values)
        final-map (coercing-merge config-map env)
        ;;Only print keys from config files.  Do not print out entire env
        print-map (into {} (filter #(contains? config-map (first %)) final-map))]
    (doseq [k (clojure.set/intersection (set (keys env))
                                        (set (keys print-map)))]
      (alter-var-root (var *config-sources*) #(assoc % k "environment")))
    final-map))

(defn- init-config-map []
  (alter-var-root (var *config-map*) (fn [_] (build-config-env))))

(defn get-config-map
  []
  (when-not *config-map*
    (init-config-map))
  *config-map*)

(defn get-configurable-options
  "This function returns all keys that are specified in .edn files, excluding
  the automatic variables such as os-*."
  []
  (-> (get-config-map)
      (keys)
      (set)
      (set/difference #{:os-arch :os-name :os-version})))

(defn get-config-table-str
  "Returns a nice string representation of the current config map."
  []
  (let [edn-config-key-set (set (keys (get-config-edn-values)))
        table (->> (get-config-map)
                   ;;Only print keys from config files.  Do not print out entire env
                   (filter #(edn-config-key-set (first %)))
                   (sort-by first)
                   (map (fn [[k v]]
                          {:key k
                           :value v
                           :source (get *config-sources* k)})))
        key-width (min 30 (apply max (map (comp count str :key) table)))
        val-width (min 30 (apply max (map (comp count str :value) table)))
        src-width (min 20 (apply max (map (comp count str :source) table)))]
    (with-out-str
      (with-bindings {#'pprint/*print-right-margin* nil}
        (println (format (str "%-" key-width "s %-" val-width "s %-" src-width "s") "Key" "Value" "Source"))
        (println (apply str (repeat (+ key-width val-width src-width 2) "-")))
        (doseq [{:keys [key value source]} table]
          (println (format (str "%-" key-width "s %-" val-width "s %-" src-width "s")
                           key (if (string? value) (format "\"%s\"" value) value) source)))))))

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
  `(let [new-map# (#'think.config.core/coercing-merge (get-config-map) (apply hash-map ~config-key-vals))
         new-sources# (->> (take-nth 2 ~config-key-vals)
                           (map (fn [new-var#] [new-var# "with-config"]))
                           (into {}))]
     (with-bindings {#'*config-map* new-map#
                     #'*config-sources* (merge *config-sources* new-sources#)}
       ~@body)))
