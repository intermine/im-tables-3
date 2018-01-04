(ns im-tables.views
  (:require [re-frame.core :as re-frame :refer [dispatch]]
            [im-tables.views.core :as main-view]))

; This function is used for testing purposes.
; Real use cases should call [im-tables.views.core/main {settings-map}]
(defn main-panel []
  (fn []
    [:div.container-fluid
     [main-view/main {:location [:test :location]
                      :service {:root "beta.flymine.org/beta"}
                      :query {:from "Gene"
                              :select ["Gene.symbol"]
                              :where [{:path "Gene.symbol"
                                       :op "LIKE"
                                       :value "M*"}]}}]]))
