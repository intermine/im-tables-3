(ns im-tables.views.dashboard.main
  (:require [im-tables.views.dashboard.pagination :as pager]
            [im-tables.views.dashboard.manager.columns.main :as column-manager]
            [im-tables.views.dashboard.manager.relationships.main :as rel-manager]
            [im-tables.views.dashboard.manager.filters.main :as filter-manager]
            [im-tables.views.dashboard.undo :as undo]
            [im-tables.views.dashboard.save :as save]
            [im-tables.views.dashboard.manager.columns.main :as saver]
            [im-tables.views.dashboard.manager.codegen.main :as codegen]
            [im-tables.views.dashboard.exporttable :as exporttable]
            [re-frame.core :refer [dispatch]]
            [oops.core :refer [ocall]]))

(defn main []
  (fn [loc response pagination]
    [:div.container-fluid
     [:div.row
      [column-manager/main loc]]
     [:div.row.im-table-toolbar
      [:div.col-sm-12
       [:div.btn-toolbar.pull-left.dashboard-buttons
        [:div.btn-group
         [:button.btn.btn-default
          {:on-click (fn []
                       ; Clear the previous state of the column manager when (re)opening
                       (dispatch [:tree-view/clear-state loc])
                       (dispatch [:modal/open loc (saver/make-modal loc)]))}
          [:i.fa.fa-columns] " Add Columns"]]
        [:div.btn-group [filter-manager/main loc]]
        [:div.btn-group [rel-manager/main loc]]
        [:div.btn-group [save/main loc]]
        [:div.btn-group [exporttable/exporttable loc]]
        [:div.btn-group [codegen/main loc]]
        [undo/main loc]]
       [:div.row.pagination-bar.pull-left
        [:div.pull-left
         [pager/main loc
          (merge pagination
                 {:total (get response :iTotalRecords)})]]
        [:label.pull-left
         {:style {:padding-left "10px"}}
         (when (:iTotalRecords response)
           (str "Showing "
                (inc (:start pagination)) " to "
                (min
                 (+ (:start pagination) (:limit pagination))
                 (:iTotalRecords response))
                " of "
                (.toLocaleString (:iTotalRecords response))

                " rows"))]]]]]))
