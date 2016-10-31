(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.graphs.histogram :as histogram]))

(defn column-summary []
  (let [summaries (subscribe [:summaries/column-summaries])]
    (fn [view]
      (let [results (get-in @summaries [view :results])]
        [:div
         [histogram/main results]
         [:div.max-height-400
          [:table.table.table-striped.table-condensed
           (into [:tbody]
                 (map (fn [{:keys [count item]}]
                        [:tr
                         [:td (if item item [:i.fa.fa-ban.mostly-transparent])]
                         [:td count]]) results))]]]))))

(defn toolbar []
  (fn [view]
    [:div.summary-toolbar
     [:i.fa.fa-sort
      {:on-click (fn [] (dispatch [:main/sort-by view]))}]
     [:i.fa.fa-times
      {:on-click (fn [] (dispatch [:main/remove-view view]))}]
     [:i.fa.fa-filter]
     [:span.dropdown
      {:on-click (fn [] (dispatch [:main/summarize-column view]))}
      [:i.fa.fa-bar-chart.dropdown-toggle {:data-toggle "dropdown"}]
      [:div.dropdown-menu
       [column-summary view]]]]))

(defn main []
  (fn [view]
    [:div
     [toolbar view]]))