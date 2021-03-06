(ns im-tables.core-test
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [im-tables.test-utils :as utils]
            [re-frame.core :as rf]
            [day8.re-frame.test :refer-macros [run-test-sync run-test-async wait-for]]
            [im-tables.events]
            [im-tables.subs]
            [clojure.string :as string]))

(use-fixtures :each utils/fixtures)

(def im-config {:service {:root "https://www.flymine.org/flymine"}
                :query {:from "Gene"
                        :select ["symbol"
                                 "secondaryIdentifier"
                                 "dataSets.description"
                                 "primaryIdentifier"
                                 "organism.name"
                                 "dataSets.name"]
                        :where [{:path "Gene.symbol"
                                 :op "CONTAINS"
                                 :value "abc"}]}
                :settings {:pagination {:limit 10}
                           :links {:vocab {:mine "flymine"}
                                   :url (fn [{:keys [mine class objectId] :as _vocab}]
                                          (string/join "/" [nil mine "report" class objectId]))}}})

(deftest load-im-tables
  (let [loc [:default]]
    (utils/after-load loc im-config
      (testing "im-table runs query"
        (let [response @(rf/subscribe [:main/query-response loc])]
          (is (some? response)))))))

(deftest column-summary
  (let [loc [:default]]
    (utils/after-load loc im-config
      (rf/dispatch-sync [:main/summarize-column loc "Gene.dataSets.description"])
      (wait-for [:main/save-column-summary]
        (testing "at least one non-empty column summary"
          (let [summaries @(rf/subscribe [:summaries/column-summaries loc])]
            (is (some (every-pred map? not-empty)
                      (map :response (vals summaries))))))))))

(deftest sort-column
  (let [loc [:default]]
    (utils/after-load loc im-config
      (rf/dispatch-sync [:main/sort-by loc "Gene.primaryIdentifier"])
      (wait-for [:main/replace-query-response]
        (testing "response can be sorted by column"
          (let [response @(rf/subscribe [:main/query-response loc])
                result (get-in response [:results 0])]
            (is (= "100132289"
                   (->> result
                        (filter #(= (:column %) "Gene.primaryIdentifier"))
                        first
                        :value)))))))))
