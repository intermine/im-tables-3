(ns im-tables.views.dashboard.save
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))

(defn main []
  (fn []
    [:button.btn.btn-default
    {:on-click (fn [])}
     "Save List"]))