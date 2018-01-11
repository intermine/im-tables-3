(ns im-tables.views
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [im-tables.views.core :as main-view]
            [reagent.core :as r]
            [reagent.dom.server :as server]))



; This function is used for testing purposes.
; Real use cases should call [im-tables.views.core/main {settings-map}]
(defn main-panel []
  (let [show? (r/atom true)]
    (fn []
      [:div.container-fluid
       [:button.btn.btn-warning "Modal"]
       [:button.btn.btn-default {:on-click (fn [] (swap! show? not))} "Toggle"]
       (when @show?
         (into [:div] (map (fn [n]
                             [main-view/main {:location [:test :location n]
                                              :service {:root "beta.flymine.org/beta"}
                                              :settings {:pagination {:limit 10}}
                                              :query {:from "Gene"
                                                      :select ["symbol"
                                                               "secondaryIdentifier"
                                                               "primaryIdentifier"
                                                               "organism.name"
                                                               "dataSets.name"]
                                                      :where [{:path "Gene.symbol"
                                                               :op "LIKE"
                                                               :value "M*"}]}
                                              #_#_:query {:from "Gene"
                                                          :select ["symbol"
                                                                   "secondaryIdentifier"
                                                                   "primaryIdentifier"
                                                                   "organism.name"
                                                                   "publications.firstAuthor"
                                                                   "dataSets.name"]
                                                          :joins ["Gene.publications"]
                                                          :size n
                                                          :sortOrder [{:path "symbol"
                                                                       :direction "ASC"}]
                                                          :where [
                                                                  {:path "secondaryIdentifier"
                                                                   :op "="
                                                                   :value "AC3.1*" ;AC3*
                                                                   :code "A"}
                                                                  ]}
                                              }]

                             ) (range 0 1)))
         #_[:div.row
            [:div.col-xs-6 [main-view/main {:location [:test :location]
                                            :service {:root "beta.flymine.org/beta"}
                                            ;:query {:from "Gene"
                                            ;        :select ["Gene.symbol"]
                                            ;        :where [{:path "Gene.symbol"
                                            ;                 :op "LIKE"
                                            ;                 :value "M*"}]}
                                            :query {:from "Gene"
                                                    :select ["symbol"
                                                             "secondaryIdentifier"
                                                             "primaryIdentifier"
                                                             "organism.name"
                                                             "publications.firstAuthor"
                                                             "dataSets.name"]
                                                    :joins ["Gene.publications"]
                                                    :size 5
                                                    :sortOrder [{:path "symbol"
                                                                 :direction "ASC"}]
                                                    :where [
                                                            {:path "secondaryIdentifier"
                                                             :op "="
                                                             :value "AC3.1*" ;AC3*
                                                             :code "A"}
                                                            ]}
                                            }]]
            #_[:div.col-xs-6 [main-view/main {:location [:test :location2]
                                              :service {:root "beta.flymine.org/beta"}
                                              ;:query {:from "Gene"
                                              ;        :select ["Gene.symbol"]
                                              ;        :where [{:path "Gene.symbol"
                                              ;                 :op "LIKE"
                                              ;                 :value "M*"}]}
                                              :query {:from "Gene"
                                                      :select ["Gene.symbol"]
                                                      :where [{:path "Gene.symbol"
                                                               :op "="
                                                               :value "AB*"}]}
                                              }]]])])))