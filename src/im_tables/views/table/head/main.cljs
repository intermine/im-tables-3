(ns im-tables.views.table.head.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.head.controls :as controls]
            [reagent.core :as reagent]
            [clojure.string :refer [join split]]
            [oops.core :refer [ocall oget]]
            [imcljs.path :as impath]))

(defn is-child-of?
  "Utility method to check if a given dom node contains another"
  [possible-child parent]
  (if parent
    (ocall parent "contains" possible-child)
    false))

(defn header []
  (let [model (subscribe [:assets/model])
        draggable? (reagent/atom true)]
    (fn [{:keys [idx header view loc dragging-over dragging-item col-count] :as header}]
      (let [drag-class (cond
                         (and (= idx dragging-over) (< idx dragging-item)) "drag-left"
                         (and (= idx dragging-over) (> idx dragging-item)) "drag-right")
            [class & path] (split header " > ")
            display-name (impath/display-name @model view)]
        [:th
         {:class drag-class
          :draggable @draggable?
          :on-mouse-down
          (fn [e]
            ;; so we don't want the filters or column summaries to be draggable.
            ;; because interacting with them via the mouse is REALLY annoying when
            ;; you accidentally drag instead of clicking.
            ;; solution: disable draggable if clicking on children of the filter/summary elements.
            (let [clicked-element (oget e "target")
                  target (oget e "currentTarget")
                  filter-clicked? (is-child-of? clicked-element (ocall target "querySelector" ".filter-view"))
                  summary-clicked? (is-child-of? clicked-element (ocall target "querySelector" ".column-summary"))]
              (reset! draggable? (not (or filter-clicked? summary-clicked?)))))
          :on-drag-over (fn [] (dispatch [:style/dragging-over loc idx]))
          :on-drag-start (fn [e]
                           (ocall e "dataTransfer.setData" "text" (str "dragging column" idx))
                           (dispatch [:style/dragging-item loc idx]))
          :on-drag-end (fn [] (dispatch ^:flush-dom [:style/dragging-finished loc]))}
         [controls/toolbar loc view idx col-count]
         [:div
          [:div (last (drop-last display-name))]
          [:div (last display-name)]]]))))
