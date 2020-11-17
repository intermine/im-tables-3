(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as reagent]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.core :as table]
            [im-tables.components.bootstrap :refer [modal]]
            [reagent.dom.server :as server]
            [oops.core :refer [ocall oget]]
            [imcljs.path :as impath]
            [im-tables.views.table.error :as error]))

(defn custom-modal []
  (fn [loc {:keys [header body footer extra-class no-fade]}]
    [:div.im-modal
     (if no-fade
       {:class :no-fade}
       {:on-mouse-down #(dispatch [:modal/close loc])})
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

(defn error-boundary
  [props & children]
  (let [caught? (reagent/atom false)
        !error (reagent/atom nil)]
    (reagent/create-class
     {:display-name "ImTablesRootErrorBoundary"
      :get-derived-state-from-error (fn [_]
                                      (reset! caught? true)
                                      #js {})
      :component-did-catch (fn [_ error info]
                             (reset! !error {:type :boundary
                                             :error error
                                             :info info}))
      :render (fn [this]
                (let [{:keys [location]} (reagent/props this)
                      clear-error! (fn []
                                     (dispatch-sync [:im-tables/restart location])
                                     (reset! !error nil)
                                     (reset! caught? false))]
                  (if @caught?
                    (when-let [error @!error]
                      [:div.im-table.relative
                       [error/failure location error
                        :clear-error! clear-error!]])
                    (into [:<>] (reagent/children this)))))})))

(defn main [location]
  (let [response (subscribe [:main/query-response location])
        pagination (subscribe [:settings/pagination location])
        modal-markup (subscribe [:modal location])
        static? (reagent/atom true)
        model (subscribe [:assets/model location])
        query (subscribe [:main/query location])
        collapsed-views (subscribe [:query-response/views-collapsed-by-joins location])]
    (fn [location]
       ; The query results are stored in a map with the result index as the key.
       ; In other words, we're not using a vector! To generate a speedy preview,
       ; get the first n results to be shown as a simple table:
       ; (get results 0), (get results 1), (get results 2) ... (get results limit)
      (let [preview-rows (map (partial get (:results @response)) (range (:limit @pagination)))]
        [error-boundary {:location location}
         [:div.im-table.relative
           ; When the mouse touches the table, set the flag to render the actual React components
          {:on-mouse-over (fn []
                            (when @static?
                              (reset! static? false)))}

          (if @static?
             ; If static (optimised) then only show an HTML representations of the React components
            [:div
              ; Force the dashboard buttons to be just HTML (their lights are on but no one is home)
             [:div {:dangerouslySetInnerHTML {"__html" (server/render-to-static-markup [dashboard/main location @response @pagination])}}]
             [table/handle-states location @response
              [:div.table-container
               [:table.table.table-condensed.table-bordered.table-striped
                ; Good old static (fast) html table:
                [:thead
                 (into [:tr]
                       (->> @collapsed-views
                            (map-indexed (fn [idx h]
                                           (let [[attrib-name parent-name] (when (and @model h)
                                                                             (rseq (impath/display-name @model h)))
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
                                               [:div parent-name]
                                               [:div attrib-name]]])))))]
                (into [:tbody] (map (fn [row]
                                      (into [:tr] (map (fn [cell]
                                                         [:td
                                                          (if-let [value (:value cell)]
                                                            [:a value]
                                                            [:a.no-value "NO VALUE"])]) row))) preview-rows))]]]]
             ; Otherwise show the interactive React components
            [:div
             [dashboard/main location @response @pagination]
             [table/main location @response @pagination]])

           ; Only show the modal when the modal subscription has a value
          (when @modal-markup [custom-modal location @modal-markup])]]))))
