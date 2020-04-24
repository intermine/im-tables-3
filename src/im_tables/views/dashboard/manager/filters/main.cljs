(ns im-tables.views.dashboard.manager.filters.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [imcljs.path :as path]
            [oops.core :refer [oget]]
            [im-tables.views.table.head.controls :as controls]))

(defn join-with-arrows
  "Make a friendly HTML path name: Gene >> Data Sets >> Publications"
  [v]
  (->> v
       (map (fn [n] [:span n]))
       (interpose [:span [:i.fa.fa-angle-double-right.fa-fw]])))

(defn constraint [loc]
  (let [model (subscribe [:assets/model loc])]
    (fn [loc {:keys [path op value code] :as const}]
      [:li.list-group-item.filter-item
       (-> [:div.filter-name]
           (into (join-with-arrows (path/display-name @model path)))
           (conj [:span.text-muted (str " (" code ")")]))
       [controls/constraint loc path const]])))

(defn constraint-form [loc]
  (into [:ul.list-group.filter-manager]
        (for [const @(subscribe [:filter-manager/filters loc])]
          ^{:key (:code const)}
          [constraint loc const])))

(defn constraint-logic [loc]
  (let [constraint-logic @(subscribe [:filter-manager/constraint-logic loc])
        on-change #(dispatch [:filter-manager/change-constraint loc
                              (oget % :target :value)])]
    [:div.form
     [:div.form-group
      [:label "Constraint Logic"]
      [:input.form-control.input-md
       {:value constraint-logic
        :on-change on-change}]]]))

(defn modal-body [loc]
  [:div
   [constraint-form loc]
   [constraint-logic loc]])

(defn modal-footer [loc]
  [:div.btn-toolbar.pull-right
   [:button.btn.btn-default
    {:on-click #(dispatch [:modal/close loc])}
    "Cancel"]
   [:button.btn.btn-success
    {:on-click (fn []
                 ; Apply the changes
                 (dispatch [:filters/save-changes loc])
                 ; Close the modal by clearing the markup from app-db
                 (dispatch [:modal/close loc]))}
    "Apply Changes"]])

(defn build-modal [loc]
  {:header [:h3 "Manage Filters"]
   :body [modal-body loc]
   :footer [modal-footer loc]})

(defn main [loc]
  [:div
   [:div.btn-group
    [:button.btn.btn-default
     {:on-click (fn []
                  ; Reset temp query
                  (dispatch [:main/set-temp-query loc])
                  ; Fetch possible values for all filters present
                  (doseq [view (->> @(subscribe [:filter-manager/filters loc])
                                    (map :path))]
                    (dispatch [:main/fetch-possible-values loc view]))
                  ; Build the modal markup and send it to app-db
                  (dispatch [:modal/open loc (build-modal loc)]))}
     [:i.fa.fa-filter] " Manage Filters"]]])
