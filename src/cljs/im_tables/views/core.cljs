(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.core :as table]
            [im-tables.views.dashboard.pagination :as pager]))

(defn main []
  (let [response   (subscribe [:main/query-response])
        pagination (subscribe [:settings/pagination])]
    (fn []
      [:div

       [:div.pull-left [:span (str "Showing "
                                 (inc (:start @pagination)) " to "
                                 (+ (:start @pagination) (:limit @pagination)) " of "
                                 (:iTotalRecords @response) " rows")]]
       [:div.pull-right
        [pager/main (merge @pagination {:inc-fn (fn [] (dispatch [:settings/update-page inc]))
                                        :dec-fn (fn [] (dispatch [:settings/update-page dec]))})]]

       [table/main @response]])))