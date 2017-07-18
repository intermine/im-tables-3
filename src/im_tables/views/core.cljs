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
      {
       ;:component-will-mount
       ;(fn [e]
       ;  (if (and loc state)
       ;    (dispatch [:im-tables.main/replace-all-state loc state])))
       ;:component-will-update
       ;(fn [this new-arg-v]
       ;  (let [[_ l old-state] (reagent/argv this)
       ;        [_ l new-state] new-arg-v]
       ;    ;(.log js/console "O" (reagent/argv this))
       ;    ;(.log js/console "N" new-arg-v)
       ;    ;(.log js/console "old-state" old-state)
       ;    ;(.log js/console "new-state" new-state)
       ;    (if (not= old-state new-state)
       ;      (do
       ;        (.log js/console "UPDATING THIS" new-state)
       ;        (dispatch [:im-tables.main/replace-all-state loc new-state])))))
       :reagent-render
       (fn [loc]
         [:div.im-table.relative
          ; Cover the app whenever it's thinking
          [table-thinking @overlay?]

          ;[:button.btn.btn-default {:on-click (fn [] (dispatch [:printdb]))} "Log DB"]
          ; The dashboard above the table (controls
          [dashboard/main loc @response @pagination]
          ; The actual table

          ;[:div.ontop "t"]
          [table/main loc @response @pagination]
          ; Use just one modal and change its contents dynamically
          [modal @modal-markup]])})))