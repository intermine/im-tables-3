(ns im-tables.views.dashboard.pagination
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :refer [split]]
            [oops.core :refer [oget]]))

(def show-amounts (list 10 20 50))



(defn main []
  (fn [loc {:keys [start limit total]}]
    [:span.pagination-bar
     [:div.btn-toolbar
      [:div.btn-group
       [:label "Show"]]
      [:div.btn-group
       (into [:select.form-control
              {:value limit
               :on-change (fn [e]
                            (dispatch [:imt.settings/update-pagination-limit loc (js/parseInt (oget e :target :value))]))}]
             (concat
               (map (fn [a]
                     [:option {:value a} a])
                    (take-while (partial > total) show-amounts))
               (list [:option {:value total} (str "All (" total ")")])))]
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




