(ns im-tables.views
  (:require [re-frame.core :as re-frame]
            [im-tables.views.core :as main-view]
            [reagent.core :as r]
            [clojure.string :as string]
            [reagent.dom.server :as server]
            [oops.core :refer [ocall]]))

(def humanmine-config
  {:service {:root "https://www.humanmine.org/humanmine"}
   :query {:from "Gene"
           :select ["symbol"
                    "secondaryIdentifier"
                    "dataSets.description"
                    "primaryIdentifier"
                    "organism.name"
                    "dataSets.name"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "humanmine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def flymine-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "Gene"
           :select ["symbol"
                    "secondaryIdentifier"
                    "primaryIdentifier"]
           :where [{:path "Gene"
                    :op "LOOKUP"
                    :value "eve"}]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def yeastmine-config
  {:service {:root "https://yeastmine.yeastgenome.org/yeastmine"}
   :query {:from "Protein"
           :select ["symbol"
                    "secondaryIdentifier"
                    "primaryIdentifier"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "yeastmine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def testmine-config
  {:service {:root "localhost:8080/intermine-demo"}
   :query {:from "Employee"
           :select ["name"
                    "department.name"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "testmine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def biotestmine-config
  {:service {:root "http://localhost:8080/biotestmine"}
   :query {:from "Gene"
           :select ["symbol"
                    "secondaryIdentifier"
                    "dataSets.description"
                    "primaryIdentifier"
                    "organism.name"
                    "dataSets.name"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "biotestmine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def covidmine-config
  {:service {:root "https://test.intermine.org/covidmine"}
   :query {:from "Cases"
           :select ["date"
                    "totalConfirmed"
                    "totalDeaths"
                    "newConfirmed"
                    "newDeaths"
                    "geoLocation.country"
                    "geoLocation.state"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "covidmine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

;; Query with subclass constraint
(def subclass-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "Gene"
           :select ["Gene.secondaryIdentifier",
                    "Gene.symbol",
                    "Gene.primaryIdentifier",
                    "Gene.organism.name",
                    "Gene.interactions.participant2.alleles.primaryIdentifier",
                    "Gene.interactions.participant2.alleles.symbol",
                    "Gene.interactions.participant2.alleles.alleleClass",
                    "Gene.interactions.participant2.alleles.organism.name"]
           :where [{:path "Gene"
                    :op "LOOKUP"
                    :value "eve"}
                   {:path "Gene.interactions.participant2"
                    :type "Gene"}]}
   :settings {:pagination {:limit 10}
              :compact true
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def outer-join-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "Gene"
           :select ["Gene.secondaryIdentifier",
                    "Gene.symbol",
                    "Gene.primaryIdentifier",
                    "Gene.organism.name",
                    "Gene.interactions.participant2.alleles.primaryIdentifier",
                    "Gene.interactions.participant2.alleles.symbol",
                    "Gene.interactions.participant2.alleles.alleleClass",
                    "Gene.interactions.participant2.alleles.organism.name"]
           :where [{:path "Gene"
                    :op "LOOKUP"
                    :value "eve"}
                   {:path "Gene.interactions.participant2"
                    :type "Gene"}
                   {:path "Gene.interactions.participant2.alleles.symbol"
                    :op "CONTAINS"
                    :value "ab"}]
           :joins ["interactions"]}
   :settings {:pagination {:limit 10}
              :compact true
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def pickitems-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "Gene"
           :select ["symbol",
                    "secondaryIdentifier",
                    "goAnnotation.ontologyTerm.identifier",
                    "goAnnotation.ontologyTerm.name",
                    "goAnnotation.evidence.code.code",
                    "goAnnotation.ontologyTerm.namespace",
                    "goAnnotation.qualifier"]
           :where [{:path "Gene"
                    :op "LOOKUP"
                    :value "Notch"}]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

;; This query has a very wide column, making it great for testing overflow.
(def flymine-wide-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "Gene"
           :select ["primaryIdentifier"
                    "symbol"
                    "regulatoryRegions.primaryIdentifier"
                    "regulatoryRegions.chromosome.primaryIdentifier"
                    "regulatoryRegions.sequenceOntologyTerm.name"
                    "regulatoryRegions.chromosomeLocation.end"
                    "regulatoryRegions.chromosomeLocation.start"
                    "regulatoryRegions.dataSets.dataSource.name"
                    "regulatoryRegions.sequence.residues"]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def flymine-url-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "DataSet"
           :select ["DataSet.name",
                    "DataSet.url"]
           :where [{:path "DataSet.url"
                    :op "IS NOT NULL"}]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

(def humanmine-config-2
  {:service {:root "https://www.humanmine.org/humanmine"}
   :query {:from "Gene"
           :select ["Gene.symbol"
                    "Gene.primaryIdentifier"
                    "Gene.organism.name"]
           :where [{:path "Gene.symbol"
                    :op "LIKE"
                    :value "*g"}]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "humanmine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

;; One of the columns contain True/False values.
(def true-false-config
  {:service {:root "https://www.flymine.org/flymine"}
   :query {:from "Gene"
           :select ["Gene.name",
                    "Gene.mRNAExpressionResults.stageRange",
                    "Gene.mRNAExpressionResults.expressed",
                    "Gene.mRNAExpressionResults.mRNAExpressionTerms.name",
                    "Gene.mRNAExpressionResults.dataSet.dataSource.name"]
           :where [{:path "Gene.organism.name",
                    :op "=",
                    :value "Drosophila melanogaster"}
                   {:path "Gene.mRNAExpressionResults.dataSet.name",
                    :op "=",
                    :value "BDGP in situ data set"}
                   {:path "Gene",
                    :op "LOOKUP",
                    :value "runt"}]}
   :settings {:pagination {:limit 10}
              :links {:vocab {:mine "flymine"}
                      :url (fn [{:keys [mine class objectId] :as _vocab}]
                             (string/join "/" [nil mine "report" class objectId]))}}})

; This function is used for testing purposes.
; When using im-tables in real life, you could call the view like so:
; [im-tables.views.core/main {:location ... :service ... :query ...}]

; Increase this range to produce N number of tables on the same page
; (useful for stress testing)


(def number-of-tables 1)
(defn reboot-tables-fn []
  (dotimes [n number-of-tables]
    (re-frame/dispatch-sync [:im-tables/load [:test :location n] flymine-url-config])))

(defn main-panel []
  (let [show? (r/atom true)]
    (fn []
      [:div.container-fluid
       [:div.container
        [:div.panel.panel-info
         [:div.panel-heading (str "Global Test Controls for " number-of-tables " tables")]
         [:div.panel-body
          [:div.btn-toolbar
           [:div.btn-group
            [:button.btn.btn-default {:on-click reboot-tables-fn}
             "Reboot Tables"]]
           [:div.btn-group
            [:button.btn.btn-default {:on-click (fn [] (swap! show? not))}
             (if @show? "Unmount Tables" "Mount Tables")]]]]]]
       (when @show?
         (into [:div {:style {:max-width "100vw"
                              :background-color "white"
                              :border "1px solid #ccc"}}]
               (->> (range 0 number-of-tables)
                    (map (fn [n] [main-view/main [:test :location n]])))))])))
