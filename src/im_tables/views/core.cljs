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

(defn main [loc state]
  (let [response     (subscribe [:main/query-response loc])
        pagination   (subscribe [:settings/pagination loc])
        overlay?     (subscribe [:style/overlay? loc])
        modal-markup (subscribe [:modal loc])]
    (reagent/create-class
      {:component-will-mount
       (fn [e]
         (if loc
           (do
             ;(println "FOUND PATH" loc)
             (dispatch [:im-tables.main/replace-all-state loc state]))))
       :reagent-render
       (fn []
         (.log js/console "pagination" @pagination)
         [:div.im-table.relative
          ; Cover the app whenever it's thinking
          [table-thinking @overlay?]
          ; Debug
          [:button.btn.btn-default {:on-click (fn [] (dispatch [:printdb]))} "Log DB"]
          ; The dashboard above the table (controls
          [dashboard/main loc @response @pagination]
          ; The actual table
          [table/main loc @response @pagination]
          ; Use just one modal and change its contents dynamically
          [modal @modal-markup]])})))