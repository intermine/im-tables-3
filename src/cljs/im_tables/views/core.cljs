(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.core :as table]
            ))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn main []
  (let [response   (subscribe [:main/query-response])
        pagination (subscribe [:settings/pagination])
        overlay? (subscribe [:style/overlay?])]
    (fn []
      [:div.relative
       [css-transition-group
        {:transition-name          "fade"
         :transition-enter-timeout 50
         :transition-leave-timeout 50}
        (if @overlay?
          [:div.overlay
           [:i.fa.fa-cog.fa-spin.fa-4x.fa-fw]])]
       [dashboard/main @response @pagination]
       [table/main @response]])))