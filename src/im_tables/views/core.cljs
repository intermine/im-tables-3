(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.core :as table]
            [im-tables.components.bootstrap :refer [modal]]
            [reagent.dom.server :as server]
            [oops.core :refer [ocall]]
            [imcljs.path :as impath]))

(def css-transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransitionGroup))

(defn table-thinking []
  (fn [show?]
    [css-transition-group
     {:transition-name          "fade"
      :transition-enter-timeout 50
      :transition-leave-timeout 50}
     (if show?
       [:div.overlay
        [:i.fa.fa-cog.fa-spin.fa-4x.fa-fw]])]))

(defn custom-modal []
  (fn [loc {:keys [header body footer extra-class]}]
    [:div.im-modal
     {:on-mouse-down (fn [e]
                       (dispatch [:prep-modal loc nil]))}
     [:div.im-modal-content
      {:class extra-class
       :on-mouse-down (fn [e]
                        (ocall e :stopPropagation))}
      [:div.modal-dialog.
       [:div.modal-content
        [:div.modal-header header]
        [:div.modal-body body]
        [:div.modal-footer footer]]]]]))

(defn constraint-has-path? [view constraint]
  (= view (:path constraint)))

(defn main [location]
  (let [response (subscribe [:main/query-response location])
        pagination (subscribe [:settings/pagination location])
        overlay? (subscribe [:style/overlay? location])
        modal-markup (subscribe [:modal location])
        static? (reagent/atom true)
        model (subscribe [:assets/model location])
        query (subscribe [:main/query location])
        collapsed-views (subscribe [:query-response/views-collapsed-by-joins location])
        views (subscribe [:query-response/views location])]
    (reagent/create-class
     {:reagent-render
      (fn [location]
         ; The query results are stored in a map with the result index as the key.
         ; In other words, we're not using a vector! To generate a speedy preview,
         ; get the first n results to be shown as a simple table:
         ; (get results 0), (get results 1), (get results 2) ... (get results limit)
        (let [preview-rows (map (partial get (:results @response)) (range (:limit @pagination)))]
          [:div.im-table.relative
            ; When the mouse touches the table, set the flag to render the actual React components
           {:on-mouse-over (fn []
                             (when (and @static? (some? @response))
                               (dispatch [:main/deconstruct location])
                               (doseq [event (map (fn [view] [:main/summarize-column location view]) @views)]
                                 (dispatch event))
                               (reset! static? false)))}

           (if @static?
              ; If static (optimised) then only show an HTML representations of the React components
             [:div
               ; Force the dashboard buttons to be just HTML (their lights are on but no one is home)
              [:div {:dangerouslySetInnerHTML {"__html" (server/render-to-static-markup [dashboard/main location @response @pagination])}}]
              [:table.table.table-condensed.table-bordered.table-striped
                ; Good old static (fast) html table:
               [:thead
                (into [:tr]
                      (->> @collapsed-views
                           (map-indexed (fn [idx h]
                                          (let [display-name (when (and @model h) (impath/display-name @model h))
                                                active-filters (not-empty (filter (partial constraint-has-path? h) (:where @query)))]
                                             ; This is a simple HTML representation of
                                             ; im-tables.views.table.head.controls/toolbar
                                             ; If you modify this form or the one in the toolbar, please remember to modify both
                                             ; or they won't look the same when the table is activated
                                            [:th
                                             [:div.summary-toolbar
                                              [:i.fa.fa-sort.sort-icon]
                                              [:i.fa.fa-times.remove-icon]
                                              [:i.fa.fa-filter.dropdown-toggle.filter-icon
                                               {:class (when active-filters "active-filter")}]
                                              [:i.fa.fa-bar-chart.dropdown-toggle {:data-toggle "dropdown"}]]
                                             [:div
                                              [:div (last (drop-last display-name))]
                                              [:div (last display-name)]]])))))]
               (into [:tbody] (map (fn [row]
                                     (into [:tr] (map (fn [cell]
                                                        [:td
                                                         (if-let [value (:value cell)]
                                                           [:a value]
                                                           [:a.no-value "NO VALUE"])]) row))) preview-rows))]]
              ; Otherwise show the interactive React components
             [:div
              [dashboard/main location @response @pagination]
              [table/main location @response @pagination]])

            ; Only show the modal when the modal subscription has a value
           (when @modal-markup [custom-modal location @modal-markup])]))})))

            ; Cover the app whenever it's thinking
            ;[table-thinking @overlay?]

