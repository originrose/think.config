(ns think.config.config-test
  (:require [clojure.set :as set]
            [clojure.string :as s]
            [clojure.test :refer :all]
            [think.config.core :refer :all]))

(deftest config-test
  (testing "Config Test"
     (is (string? (get-config :os-arch)))
     (is (thrown? IllegalArgumentException (get-config :some-bs-val)))))

(deftest types-test
  (testing "Entries are properly coerced"
     (is (integer? (get-config :app-config-overwrite)))
     (is (= (get-config :app-config-overwrite) 1))
     (is (= (get-config :user-config-overwrite) 2))))

(deftest with-config-test
  (testing "Make sure with-config can coerce values properly."
    (with-config [:user-config-overwrite "3"]
      (is (= (get-config :user-config-overwrite) 3)))))

(deftest profile-env-test
  (testing "Make sure entries in the profile env work."
    (is (= (get-config :env-config-overwrite) true))))

(deftest print-config-map
  (is (string? (get-config-table-str))))

(deftest configurable-options-test
  (is (empty? (set/intersection #{:os-arch :os-name :os-version} (get-configurable-options)))))

(deftest with-config-updates-sources-test
  (testing "Make sure with-config updates the soruces map."
    (with-config [:user-config-overwrite "3"]
      (->> (get-config-table-str)
           (s/split-lines)
           (filter #(.contains % "user-config-overwrite"))
           (first)
           ((fn [x] (s/split x #"\s+")))
           (last)
           (= "with-config")
           (is)))))

(deftest environ-updates-sources
  (->> (get-config-table-str)
       (s/split-lines)
       (filter #(.contains % "env-config-overwrite"))
       (first)
       ((fn [x] (s/split x #"\s+")))
       (last)
       (= "environment")
       (is)))

(deftest string-false-is-false
  (with-config [:boolean "false"]
    (is (false? (get-config :boolean))))
  (with-config [:boolean "true"]
    (is (true? (get-config :boolean)))))

(deftest read-string-test
  (with-config [:m "{:a :b}"]
    (is (nil? (:a (get-config :m))))
    (is (= :b (:a (get-config :m true))))))

(deftest file-order-test
  (testing "Make sure files are merged in reverse-alphabetical order."
    (is (= (get-config :file-order-overwrite) true))))

(deftest complex-types-test
  (testing "Make sure that complex types can be properly handled."
    (is (map? (get-config :complex-type-map)))
    (is (seq? (get-config :complex-type-seq)))
    (is (vector? (get-config :complex-type-vec)))
    (is (map? (get-config :complex-type-env-overwrite-map)))
    (is (= (:b (get-config :complex-type-env-overwrite-map)) 3))
    (is (= (last (get-config :complex-type-env-overwrite-seq)) :a))
    (is (= (last (get-config :complex-type-env-overwrite-vec)) :a))

    (with-config [:complex-type-map "{:a 1 :b 4}"]
      (is (= (:b (get-config :complex-type-map)) 4)))

    (is (thrown? IllegalArgumentException (with-config [:complex-type-map [:a :b :c]])))
    (is (thrown? IllegalArgumentException (with-config [:complex-type-map "[:a :b :c]"])))))
