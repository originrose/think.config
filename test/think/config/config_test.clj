(ns think.config.config-test
  (:require
      [clojure.test :refer :all]
      [think.config.core :refer :all]))

(deftest config-test
  (testing "Config Test"
     (is (string? (get-config :os-arch)))
     (is (thrown? IllegalArgumentException (get-config :some-bs-val)))))
