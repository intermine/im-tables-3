(ns im-tables.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [im-tables.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
