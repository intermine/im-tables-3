(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.graphs.histogram :as histogram]
            [oops.core :refer [oget ocall!]]))


(defn filter-input []
  (fn []
    [:input.form-control {:type "text"}]))

(defn column-summary []
  (let [summaries (subscribe [:summaries/column-summaries])]
    (fn [view]
      (let [results (get-in @summaries [view :results])]
        [:form.form.min-width-250
         [histogram/main results]
         [filter-input]
         [:div.max-height-400
          [:table.table.table-striped.table-condensed
           [:thead [:tr [:th] [:th "Item"] [:th "Count"]]]
           (into [:tbody]
                 (map (fn [{:keys [count item]}]
                        [:tr
                         {:on-click (fn [] (dispatch [:select/toggle-selection view item]))}
                         [:td [:div
                               [:label
                                [:input
                                 {:type "checkbox"}]]]]
                         [:td (if item item [:i.fa.fa-ban.mostly-transparent])]
                         [:td count]]) results))]]
         [:div.btn-toolbar
          [:div.btn-group
           [:button.btn.btn-primary {:type "button"} "Filter"]]]]))))

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