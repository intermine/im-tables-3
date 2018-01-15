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
         (into [:div]
               ; Increase these range to produce N number of tables on the same page
               ; (useful for stress testing)
               (->> (range 0 30)
                    (map (fn [n]
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
                                                         :value "M*"}]}}])))))])))