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
     {:transition-name          "fade"
      :transition-enter-timeout 50
      :transition-leave-timeout 50}
     (if show?
       [:div.overlay
        [:i.fa.fa-cog.fa-spin.fa-4x.fa-fw]])]))

(defn main []
  (let [response     (subscribe [:main/query-response])
        pagination   (subscribe [:settings/pagination])
        overlay?     (subscribe [:style/overlay?])
        modal-markup (subscribe [:modal])]
    (reagent/create-class
      {:component-will-mount
       (fn [e]
         (let [{:keys [path state]} (reagent/props e)]
           (dispatch [:replace-all-state state path])))
       :reagent-render
       (fn []
         [:div.relative
          ; Cover the app whenever it's thinking
          [table-thinking @overlay?]
          ; The dashboard above the table (controls
          [dashboard/main @response @pagination]
          ; The actual table
          [table/main @response]
          ; Use just one modal and change its contents dynamically
          [modal @modal-markup]])})))