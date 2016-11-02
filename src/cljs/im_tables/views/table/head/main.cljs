(ns im-tables.views.table.head.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.head.controls :as controls]
            [clojure.string :refer [join split]]))

(defn header []
  (let [dragging-item (subscribe [:style/dragging-item])
        dragging-over (subscribe [:style/dragging-over])]
    (fn [{:keys [idx header view]}]
      (let [drag-class (cond
                         (and (= idx @dragging-over) (< idx @dragging-item)) "drag-left"
                         (and (= idx @dragging-over) (> idx @dragging-item)) "drag-right")
            [class & path] (split header " > ")]
        [:th
         {:class drag-class
          :draggable     true
          :on-drag-over  (fn [] (dispatch [:style/dragging-over idx]))
          :on-drag-start (fn [] (dispatch [:style/dragging-item idx]))
          :on-drag-end   (fn [] (dispatch [:style/dragging-finished]))}
         [controls/main view]
         [:div
          [:div class]
          [:div (join " . " path)]]]))))