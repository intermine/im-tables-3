(ns im-tables.views.dashboard.pagination
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :refer [split]]))

(defn main []
  (fn [{:keys [start limit total inc-fn dec-fn]}]
    [:span.pagination-bar
     [:div.btn-toolbar
      [:div.btn-group
       [:button.btn.btn-default
        {:disabled (< start 1)
         :on-click (fn [] (dispatch [:settings/update-pagination-fulldec]))}
        [:span.glyphicon.glyphicon-step-backward]]
       [:button.btn.btn-default
        {:disabled (< start 1)
         :on-click (fn [] (dispatch [:settings/update-pagination-dec]))}
        [:span.glyphicon.glyphicon-triangle-left]]]
      [:div.btn-group
       [:div (str "Page " (inc (/ start limit)))]]
      [:div.btn-group
       [:button.btn.btn-default
        {:on-click (fn [] (dispatch ^:flush-dom [:settings/update-pagination-inc]))}
        [:span.glyphicon.glyphicon-triangle-right]]
       [:button.btn.btn-default
        {:on-click (fn [] (dispatch [:settings/update-pagination-fullinc]))}
        [:span.glyphicon.glyphicon-step-forward]]]]]))




