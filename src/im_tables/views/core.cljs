(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.core :as table]
            [im-tables.components.bootstrap :refer [modal]]
            [reagent.dom.server :as server]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn table-thinking []
  (fn [show?]
    [css-transition-group
     {:transition-name "fade"
      :transition-enter-timeout 50
      :transition-leave-timeout 50}
     (if show?
       [:div.overlay
        [:i.fa.fa-cog.fa-spin.fa-4x.fa-fw]])]))



(defn main [{:keys [location]} state]
  (let [response (subscribe [:main/query-response location])
        pagination (subscribe [:settings/pagination location])
        overlay? (subscribe [:style/overlay? location])
        modal-markup (subscribe [:modal location])
        static? (reagent/atom true)]
    (reagent/create-class
      {
       :component-will-mount (fn [this] (dispatch [:im-tables/boot location (reagent/props this)]))
       :reagent-render
       (fn [{:keys [location query]}]

         ; The query results are stored in a map with the result index as the key.
         ; In other words, we're not using a vector! To generate a speedy preview,
         ; get the first n results to be shown as a simple table:
         ; (get results 0), (get results 1), (get results 2) ... (get results limit)
         (let [preview-rows (map (partial get (:results @response)) (range (:limit @pagination)))]
           [:div.im-table.relative
            ; When the mouse touches the table, set the flag to render the actual React components
            {:on-mouse-over (fn [] (reset! static? false))}

            (if @static?
              ; If static (optimised) then only show an HTML representations of the React components
              [:div
               ; Force the dashboard buttons to be just HTML (their lights are on but no one is home)
               [:div {:dangerouslySetInnerHTML {"__html" (server/render-to-static-markup [dashboard/main location @response @pagination])}}]
               [:table.table.table-condensed.table-bordered.table-striped
                ; Good old static (fast) html table:
                (into [:tbody] (map (fn [row]
                                      (into [:tr] (map (fn [cell]
                                                         [:td (:value cell)]) row))) preview-rows))]]
              ; Otherwise show the interactive React components
              [:div
               [dashboard/main location @response @pagination]
               [table/main location @response @pagination]])


            ; Cover the app whenever it's thinking
            ;[table-thinking @overlay?]

            ;[:button.btn.btn-default {:on-click (fn [] (dispatch [:printdb]))} "Log DB"]
            ; The dashboard above the table (controls

            ; The actual table
            ;[dashboard/main location @response @pagination]

            ;[:div.ontop "t"]


            ; Use just one modal and change its contents dynamically
            [modal @modal-markup]
            ]))})))