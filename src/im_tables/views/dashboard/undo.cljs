(ns im-tables.views.dashboard.undo
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn main []
  (let [undos (subscribe [:undos?])]
    (fn []
      [:button.btn.btn-default
       {:disabled (not @undos)
        :on-click (fn []
                    (dispatch [:undo])
                    (dispatch [:purge-redos]))} "Undo"])))