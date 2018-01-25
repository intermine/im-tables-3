(ns im-tables.views.dashboard.undo
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn main [loc]
  (let [undos (subscribe [:undos? loc])]
    (fn []
      [:button.btn.btn-default
       {:disabled (not @undos)
        :on-click (fn []
                    (dispatch [:undo])
                    (dispatch [:purge-redos]))}
       [:i.fa.fa-undo] " Undo"])))