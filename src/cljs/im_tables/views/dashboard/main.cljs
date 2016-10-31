(ns im-tables.views.dashboard.main
  (:require [im-tables.views.dashboard.pagination :as pager]
            [im-tables.views.dashboard.undo :as undo]))

(defn main []
  (fn [response pagination]
    [:div
     [:div.pull-right [undo/main]]
     [:div.pull-left [:span (str "Showing "
                                 (inc (:start pagination)) " to "
                                 (+ (:start pagination) (:limit pagination)) " of "
                                 (:iTotalRecords response) " rows")]]
     [:div.pull-right
      [pager/main (merge pagination {:total (get response :iTotalRecords)})]]]))