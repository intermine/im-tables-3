(ns im-tables.views.table.body.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [clojure.string :refer [split]]
            [oops.core :refer [ocall oget]]))

(defn dot-split
  "Convert a view such as Gene.organism.name into [:organism :name]
  for easy reaching into query result maps"
  [string]
  (into [] (drop 1 (map keyword (split string ".")))))

(defn generate-summary-table [{:keys [value column-headers] :as summary}]
  [:table.table.table-striped.table-condensed.table-bordered
   (into [:tbody {:style {:font-size "0.9em"}}]
         (map-indexed
           (fn [idx column-header]
             (if-let [v (get-in value (dot-split (get (:views summary) idx)))]
               (let [v (if (and (string? v) (> (count v) 200)) (str (clojure.string/join (take 200 v)) "...") v)]
                 [:tr
                 [:td (clojure.string/join " > " (drop 1 (clojure.string/split column-header " > ")))]
                 [:td v]])))
           column-headers))])

(defn table-cell [idx {id :id}]
  (let [show-tooltip? (reagent/atom false)
        dragging-item (subscribe [:style/dragging-item])
        dragging-over (subscribe [:style/dragging-over])
        my-dimensions (reagent/atom {})]


    (reagent/create-class
      {:name                   "Table Cell"
       :component-will-unmount (fn [])
       :component-did-mount    (fn [this]
                                 (let [bb (ocall (reagent/dom-node this) "getBoundingClientRect")]
                                   (swap! my-dimensions assoc
                                          :width (oget bb "width")
                                          :height (oget bb "height")
                                          :left (oget bb "left")
                                          :right (oget bb "right")
                                          :top (oget bb "top")
                                          :bottom (oget bb "bottom"))))
       :reagent-render         (let [summary (subscribe [:summary/item-details id])]
                                 (fn [idx {:keys [value id] :as c}]

                                   (let [summary-table (generate-summary-table @summary)
                                         drag-class    (cond
                                                         (and (= idx @dragging-over) (< idx @dragging-item)) "drag-left"
                                                         (and (= idx @dragging-over) (> idx @dragging-item)) "drag-right")]
                                     [:td.cell
                                      {:on-mouse-enter (fn []
                                                         (dispatch [:main/summarize-item c])
                                                         (reset! show-tooltip? true))
                                       :on-mouse-leave (fn []
                                                         (reset! show-tooltip? false))
                                       :style          {:position "relative"}
                                       :class          drag-class}
                                      [:span (if value value [:i.fa.fa-ban.mostly-transparent])]
                                      (if @show-tooltip?
                                        [:div.test
                                         [:div.arrow_box
                                          {:on-mouse-enter (fn [] (reset! show-tooltip? false))
                                           :style          {:position "absolute"
                                                            :top      (:height @my-dimensions)}}
                                          summary-table]]

                                        ;[inner-tooltip @mystate show? (:data-content attributes)]
                                        )])))})))

(defn table-row [row]
  (into [:tr]
        (map-indexed (fn [idx c]
                       ^{:key (str idx (:id c) (:column c))} [table-cell idx c])) row))