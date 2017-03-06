(ns think.config.config-test
  (:require
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
