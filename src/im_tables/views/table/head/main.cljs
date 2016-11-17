(ns im-tables.views.table.head.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.head.controls :as controls]
            [clojure.string :refer [join split]]))

(defn header []
  (fn [{:keys [idx header view loc dragging-over dragging-item] :as header}]
    (let [drag-class (cond
                       (and (= idx dragging-over) (< idx dragging-item)) "drag-left"
                       (and (= idx dragging-over) (> idx dragging-item)) "drag-right")
          [class & path] (split header " > ")]
      [:th
       {:class         drag-class
        :draggable     true
        :on-drag-over  (fn [] (dispatch [:style/dragging-over loc idx]))
        :on-drag-start (fn [] (dispatch [:style/dragging-item loc idx]))
        :on-drag-end   (fn [] (dispatch ^:flush-dom [:style/dragging-finished loc]))}
       [controls/main view]
       [:div
        [:div class]
        [:div (join " . " path)]]])))