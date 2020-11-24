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
            [re-frame.core :refer [dispatch subscribe]]))

(defn main [loc]
  (let [compact? (subscribe [:settings/compact loc])]
    (fn [loc response pagination]
      [:div.dashboard
       (when-not @compact?
         [:div.dashboard-buttons
          [:div.btn-toolbar
           [:div.btn-group
            [:button.btn.btn-default
             {:on-click (fn []
                          ; Clear the previous state of the column manager when (re)opening
                          (dispatch [:tree-view/clear-state loc])
                          (dispatch [:modal/open loc (saver/make-modal loc)]))}
             [:i.fa.fa-columns] " Add Columns"]]
           [:div.btn-group [filter-manager/main loc]]
           [:div.btn-group [rel-manager/main loc]]
           [undo/main loc]]
          [:div.btn-toolbar
           [:div.btn-group [save/main loc]]
           [:div.btn-group [codegen/main loc]]
           [:div.btn-group [exporttable/exporttable loc]]]])
       (when @compact?
         [:button.btn.btn-default.dashboard-expand
          {:on-click #(dispatch [:main/expand-compact loc])}
          [:i.fa.fa-chevron-up] " Options"])
       [:div.pagination-bar
        [:label.pagination-label
         (when (:iTotalRecords response)
           (str "Showing "
                (inc (:start pagination)) " to "
                (min
                  (+ (:start pagination) (:limit pagination))
                  (:iTotalRecords response))
                " of "
                (.toLocaleString (:iTotalRecords response))

                " rows"))]
        [pager/main loc
         (merge pagination
                {:total (get response :iTotalRecords)})]]])))
