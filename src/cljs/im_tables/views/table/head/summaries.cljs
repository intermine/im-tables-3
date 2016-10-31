(ns im-tables.views.table.head.summaries
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn toolbar []
  (fn [view]
    [:div.summary-toolbar
     [:i.fa.fa-sort
      {:on-click (fn [] (dispatch [:main/sort-by view]))}]
     [:i.fa.fa-times
      {:on-click (fn [] (dispatch [:main/remove-view view]))}]
     [:i.fa.fa-filter]
     [:i.fa.fa-bar-chart]]))

(defn main []
  (fn [view]
    [:div
     [toolbar view]]))