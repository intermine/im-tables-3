(ns im-tables.views.dashboard.undo
  (:require [re-frame.core :refer [dispatch]]))

(defn main []
  (fn []
    [:div.btn-toolbar
     [:button.btn.btn-default
      {:on-click (fn [] (dispatch [:undo]))} "Undo"]]))