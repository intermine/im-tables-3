(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.core :as table]
            [im-tables.components.bootstrap :refer [modal]]))

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
        modal-markup (subscribe [:modal location])]
    (reagent/create-class
      {
       :component-will-mount (fn [this]
                               (dispatch [:im-tables/sync location (reagent/props this)])
                               )

       :reagent-render
       (fn [{:keys [location query]}]
         [:div.im-table.relative
          ; Cover the app whenever it's thinking
          [table-thinking @overlay?]

          ;[:button.btn.btn-default {:on-click (fn [] (dispatch [:printdb]))} "Log DB"]
          ; The dashboard above the table (controls
          [dashboard/main location @response @pagination]
          ; The actual table

          ;[:div.ontop "t"]
          [table/main location @response @pagination]
          ; Use just one modal and change its contents dynamically
          [modal @modal-markup]])})))