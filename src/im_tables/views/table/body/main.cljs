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
  [:table.table.table-striped.table-condensed.table-bordered.summary-table
   (into [:tbody]
         (map-indexed
           (fn [idx column-header]
             (if-let [v (get-in value (dot-split (get (:views summary) idx)))]
               (let [v (if (and (string? v) (> (count v) 200)) (str (clojure.string/join (take 200 v)) "...") v)]
                 [:tr
                  [:td (clojure.string/join " > " (drop 1 (clojure.string/split column-header " > ")))]
                  [:td v]])))
           column-headers))])


(defn tooltip-position
  "automatically positin the tooltip within the imtables div as much as possible. "
  [table-dimensions cell-dimensions]
  (let [middle (/ (- (:right table-dimensions) (:left table-dimensions)) 2)
        is-cell-left-of-middle? (< (:left cell-dimensions) middle)]
    (if is-cell-left-of-middle?
      "tooltip-right"
      "tooltip-left")
))

(defn tooltip
  "UI component for a table cell tooltip"
  [table-dimensions cell-dimensions show-tooltip? summary-table]
    (let [tooltip-position (tooltip-position @table-dimensions @cell-dimensions)
          tooltip-height (reagent/atom 120)]
    (reagent/create-class
      {:name "Table Cell"
       :component-did-mount
       (fn [this] (reset! tooltip-height (oget (reagent/dom-node this) "clientHeight")))
       :reagent-render
         (fn [this]
              [:div.im-tooltip
                {:on-mouse-enter (fn [e] (reset! show-tooltip? false) )
                 :style
                (cond-> {:bottom (- (/ @tooltip-height 2))}
                 (= tooltip-position "tooltip-right")
                   (assoc :left (:width @cell-dimensions))
                 (= tooltip-position "tooltip-left")
                   (assoc :right (:width @cell-dimensions)))
                 :class tooltip-position}
                summary-table])})))

(defn bbox->map [bb]
    {:width (oget bb "width")
    :height (oget bb "height")
    :left (oget bb "left")
    :right (oget bb "right")
    :top (oget bb "top")
    :bottom (oget bb "bottom")})

(defn table-cell [loc idx {id :id}]
  (let [show-tooltip? (reagent/atom false)
        dragging-item (subscribe [:style/dragging-item loc])
        dragging-over (subscribe [:style/dragging-over loc])
        my-dimensions (reagent/atom {})
        table-dimensions (reagent/atom {})
        settings (subscribe [:settings/settings loc])]
    (reagent/create-class
      {:name "Table Cell"
       :component-will-unmount
             (fn [])
       :component-did-mount
             (fn [this]
               (let [bb (ocall (reagent/dom-node this) "getBoundingClientRect")
                     bb-parent-tr (ocall (oget (reagent/dom-node this) "parentElement") "getBoundingClientRect")]
                (reset! my-dimensions (bbox->map bb))
                (reset! table-dimensions (bbox->map bb-parent-tr))
                 ))
       :reagent-render
             (let [summary (subscribe [:summary/item-details loc id])]
               (fn [loc idx {:keys [value id] :as c}]
                 (let [{:keys [on-click url vocab]} (get-in @settings [:links])
                       summary-table (generate-summary-table @summary)
                       drag-class    (cond
                                       (and (= idx @dragging-over) (< idx @dragging-item)) "drag-left"
                                       (and (= idx @dragging-over) (> idx @dragging-item)) "drag-right")]
                   [:td.cell
                    {:on-mouse-enter
                            (fn []
                              (dispatch [:main/summarize-item loc c])
                              (reset! show-tooltip? true))
                     :on-mouse-leave
                            (fn [] (reset! show-tooltip? false))

                     :class drag-class}
                    [:span
                     {:on-click
                      (if on-click
                        (partial on-click ((get-in @settings [:links :url])
                             (merge (:value @summary) (get-in @settings [:links :vocab]))) ))}
                     [:a
                      (if value value [:i.fa.fa-ban.mostly-transparent])]]
                    (if @show-tooltip? [tooltip table-dimensions my-dimensions show-tooltip? summary-table]
                      )])))})))

(defn table-row [loc row]
  (into [:tr]
        (map-indexed (fn [idx c]
                       ^{:key (str idx (:id c) (:column c))} [table-cell loc idx c])) row))
