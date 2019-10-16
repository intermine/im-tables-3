(ns im-tables.core-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [im-tables.test-utils :as utils]
            [re-frame.core :as rf]
            [day8.re-frame.test :refer-macros [run-test-sync run-test-async wait-for]]
            [im-tables.events]
            [im-tables.subs]))

(use-fixtures :each utils/fixtures)

(def im-config {:service {:root "beta.humanmine.org/beta"}
                :query {:from "Gene"
                        :select ["symbol"
                                 "secondaryIdentifier"
                                 "dataSets.description"
                                 "primaryIdentifier"
                                 "organism.name"
                                 "dataSets.name"]}
                :settings {:pagination {:limit 10}
                           :links {:vocab {:mine "BananaMine"}
                                   :url (fn [vocab] (str "#/reportpage/"
                                                         (:mine vocab) "/"
                                                         (:class vocab) "/"
                                                         (:id vocab)))}}})

(deftest load-im-tables
  (run-test-async
    (let [loc [:default]]
      (rf/dispatch-sync [:im-tables/load loc im-config])
      (wait-for [:main/initial-query-response]
        (testing "im-table runs query"
          (let [response @(rf/subscribe [:main/query-response loc])]
            (is (some? response))))))))

(deftest column-summary
  (run-test-async
    (let [loc [:default]]
      (rf/dispatch-sync [:im-tables/load loc im-config])
      (wait-for [:main/initial-query-response]
        (rf/dispatch-sync [:im-tables.main/init loc])
        (wait-for [(utils/match-times {:main/save-column-summary 6
                                       :main/save-decon-count    3})]
          (testing "at least one non-empty column summary"
            (let [summaries @(rf/subscribe [:summaries/column-summaries loc])]
              (is (some (every-pred map? not-empty)
                        (map :response (vals summaries)))))))))))
