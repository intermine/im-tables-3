(ns im-tables.views.dashboard.main
  (:require [im-tables.views.dashboard.pagination :as pager]
            [im-tables.views.dashboard.manager.columns.main :as column-manager]
            [im-tables.views.dashboard.undo :as undo]
            [im-tables.views.dashboard.save :as save]
            [im-tables.views.dashboard.exporttable :as exporttable]))




(defn main []
  (fn [loc response pagination]
    [:div.container-fluid
     [:div.row
      [column-manager/main loc]]
     [:div.row.im-table-toolbar
      [:div.col-xs-6
       [:div.btn-toolbar
        [:div.btn-group
         [:button.btn.btn-default
          {:data-toggle "modal"
           :data-target "#myModal"}
           [:i.fa.fa-columns] " Add Columns"]]
        [:div.btn-group
         [save/main loc]]]]
      [:div.col-xs-6
       [:div.container-fluid
        [:div.row
         [:div.col-xs-offset-2
          [:div.pull-right
           [exporttable/exporttable]
           [:div.pull-right [pager/main loc
                             (merge pagination
                                    {:total (get response :iTotalRecords)})]]
           [:span.pull-right
            {:style {:padding-right "20px"}}
            (str "Showing "
                 (inc (:start pagination)) " to "
                 (+ (:start pagination) (:limit pagination)) " of "
                 (:iTotalRecords response) " rows")]]]]]]]]))
