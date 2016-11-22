(ns im-tables.views.dashboard.pagination
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :refer [split]]))

(defn main []
  (fn [loc {:keys [start limit total]}]
    [:span.pagination-bar
     [:div.btn-toolbar
      [:div.btn-group
       [:button.btn.btn-default
        {:disabled (< start 1)
         :on-click (fn [] (dispatch [:imt.settings/update-pagination-fulldec loc]))}
        [:span.glyphicon.glyphicon-step-backward]]
       [:button.btn.btn-default
        {:disabled (< start 1)
         :on-click (fn [] (dispatch [:imt.settings/update-pagination-dec loc]))}
        [:span.glyphicon.glyphicon-triangle-left]]]
      [:div.btn-group
       [:div (str "Page " (inc (/ start limit)))]]
      [:div.btn-group
       [:button.btn.btn-default
        {:disabled (< (- total start) limit)
         :on-click (fn [] (dispatch ^:flush-dom [:imt.settings/update-pagination-inc loc]))}
        [:span.glyphicon.glyphicon-triangle-right]]
       [:button.btn.btn-default
        {:disabled (< (- total start) limit)
         :on-click (fn [] (dispatch [:imt.settings/update-pagination-fullinc loc]))}
        [:span.glyphicon.glyphicon-step-forward]]]]]))




