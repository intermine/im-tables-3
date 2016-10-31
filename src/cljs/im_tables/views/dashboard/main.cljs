(ns im-tables.views.dashboard.main
  (:require [im-tables.views.dashboard.pagination :as pager]
            [im-tables.views.dashboard.undo :as undo]))

(defn main []
  (fn [response pagination]
    [:div.row
     [:div.col-xs-6
      [:div.pull-left [:span (str "Showing "
                                  (inc (:start pagination)) " to "
                                  (+ (:start pagination) (:limit pagination)) " of "
                                  (:iTotalRecords response) " rows")]]]
     [:div.col-xs-6
      [:div.row
       [:div.col-xs-2 [undo/main]]
       [:div.col-xs-10 [pager/main (merge pagination {:total (get response :iTotalRecords)})]]]]]))