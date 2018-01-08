(ns im-tables.views
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [im-tables.views.core :as main-view]
            [reagent.core :as r]))

; This function is used for testing purposes.
; Real use cases should call [im-tables.views.core/main {settings-map}]
(defn main-panel []
  (let [show? (r/atom true)]
    (fn []
      [:div.container-fluid
       [:button.btn.btn-default {:on-click (fn [] (swap! show? not))} "Toggle"]
       (when @show? [main-view/main {:location [:test :location]
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
                                 :size 10
                                 :sortOrder [{:path "symbol"
                                              :direction "ASC"}]
                                 :where [
                                         {:path "secondaryIdentifier"
                                          :op "="
                                          :value "AC3.1*" ;AC3*
                                          :code "A"}
                                         ]}
                         }])])))
