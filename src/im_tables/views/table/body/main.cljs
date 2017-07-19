(ns im-tables.views.table.body.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [clojure.string :refer [split join]]
            [im-tables.views.common :refer [no-value]]
            [oops.core :refer [ocall oget oset!]]
            [imcljs.path :as impath]
            [reagent.dom.server :as dom-server]
            [im-tables.components.bootstrap :as bs]))

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
  (let [middle                  (/ (- (:right table-dimensions) (:left table-dimensions)) 2)
        is-cell-left-of-middle? (< (:left cell-dimensions) middle)]
    (if is-cell-left-of-middle?
      "tooltip-right"
      "tooltip-left")
    ))

(defn tooltip
  "UI component for a table cell tooltip"
  [table-dimensions cell-dimensions show-tooltip? summary]
  (let [tooltip-position (tooltip-position @table-dimensions @cell-dimensions)
        tooltip-height   (reagent/atom 0)]
    (reagent/create-class
      {:name "Tooltip"
       :component-did-mount
       (fn [this] (reset! tooltip-height (oget (reagent/dom-node this) "clientHeight")))
       :reagent-render
       (fn [this]
         [:div.im-tooltip
          {:on-mouse-enter (fn [e] (reset! show-tooltip? false))
           :style (cond-> {:bottom (- (int (/ @tooltip-height 2)))
                           :max-width (int (/ (:width @table-dimensions) 2))}
                          (= tooltip-position "tooltip-right")
                          (assoc :left (:width @cell-dimensions))
                          (= tooltip-position "tooltip-left")
                          (assoc :right (:width @cell-dimensions)))
           :class tooltip-position}
          (generate-summary-table @summary)])})))

(defn bbox->map [bb]
  {:width (oget bb "width")
   :height (oget bb "height")
   :left (oget bb "left")
   :right (oget bb "right")
   :top (oget bb "top")
   :bottom (oget bb "bottom")})

(defn outer-join-table []
  (let [model         (subscribe [:assets/model])
        open?         (reagent/atom false)
        show-tooltip? (reagent/atom false)]
    (fn [loc data]
      (if (> 1 (count (:rows data)))
        [:span.no-join-results [no-value]]
        [:div
         [:div
          {:on-click (fn [] (swap! open? not))}
          [:i.fa.fa-table.fa-fw]
          (str " " (count (:rows data)) " " (last (impath/display-name @model (:column data))))
          [:i.fa.fa-chevron-up.fa-fw.ani (when @open? {:class "upside-down"})]]
         (when @open?
           [:div
            (-> [:table.table.table-bordered.sub-table]
                (conj [:thead (into [:tr] (map
                                            (fn [th] [:th (last (impath/display-name @model th))]) (:view data)))])
                (conj
                  (into [:tbody]
                        (map (fn [rows]
                               (into [:tr]
                                     (map (fn [{:keys [id] :as c}]
                                            [:td
                                             [:a {:on-mouse-enter (fn []
                                                                    (dispatch [:main/summarize-item loc c]))
                                                  :data-trigger "hover"
                                                  :data-content (dom-server/render-to-static-markup
                                                                  (generate-summary-table
                                                                    @(subscribe [:summary/item-details loc id])))
                                                  :data-html true
                                                  :data-placement "auto right"
                                                  :ref (fn [x] (when x (.popover (js/$ x))))}
                                              (if (:value c) (:value c) [no-value])]]))
                                     rows))
                             (:rows data)))))])]))))

(defn table-cell [loc idx {id :id}]
  (let [show-tooltip? (reagent/atom false)
        dragging-item (subscribe [:style/dragging-item loc])
        dragging-over (subscribe [:style/dragging-over loc])
        settings      (subscribe [:settings/settings loc])
        summary       (subscribe [:summary/item-details loc id])
        dom-element   (reagent/atom nil)]
    (reagent/create-class
      {:name "Table Cell"
       :component-did-update (fn []
                               (println "UPDATED" @dom-element)
                               ;(some-> x
                               ;        js/$
                               ;        (ocall :popover "destroy")
                               ;        (ocall :popover))
                               )
       :reagent-render
       (fn [loc idx {:keys [value id view rows] :as c}]
         (let [{:keys [on-click url vocab]} (get-in @settings [:links])
               drag-class (cond
                            (and (= idx @dragging-over) (< idx @dragging-item)) "drag-left"
                            (and (= idx @dragging-over) (> idx @dragging-item)) "drag-right")]
           [:td.cell
            {:on-mouse-enter
             (fn []
               (when (not rows)
                 (do
                   (dispatch [:main/summarize-item loc c])
                   (reset! show-tooltip? true))))
             :on-mouse-leave (fn [] (reset! show-tooltip? false))
             :class (str drag-class (when (> (count rows) 0) nil))}
            (if (and view rows)
              [outer-join-table loc c view]
              [:span
               {:data-trigger "hover"
                ;:data-content (dom-server/render-to-static-markup (generate-summary-table @summary))
                :data-html true
                :data-container "body"
                :data-placement "auto right"
                :ref (fn [el] (when el (reset! dom-element el))


                       ;(when x (ocall (js/$ x) :popover "destroy"))
                       )
                :on-click (when (and on-click value)
                            (partial on-click ((get-in @settings [:links :url])
                                                (merge (:value @summary) (get-in @settings [:links :vocab])))))}
               (println "BOOP" @summary)
               [:a (if value value [no-value])]])
            ]))})))






;(defn poppable []
;  (let [dom (reagent/atom nil)]
;    (reagent/create-class
;      {:component-did-mount (fn [this]
;                              ; Initiate popover functionality
;                              (some-> @dom (ocall :popover)))
;       :component-did-update (fn [this]
;                               ; Reset the popover content to the summary
;                               (-> @dom
;                                   (ocall :data "bs.popover")
;                                   (oset! :options :content (dom-server/render-to-static-markup (generate-summary-table (:summary (reagent/props this))))))
;                               ; When the popover is open...
;                               (when (some true? (-> @dom
;                                                     (ocall :data "bs.popover")
;                                                     (oget :inState)
;                                                     js->clj
;                                                     vals))
;                                 ; ...then re-open it with the new content
;                                 (-> @dom (ocall :popover "show"))))
;       :reagent-render (fn [{:keys [loc idx data summary]}]
;                         [:td
;                          [:span
;                           {:on-mouse-enter (fn [] (dispatch [:main/summarize-item loc data]))
;                            :data-trigger "hover"
;                            :data-html true
;                            :data-container "body"
;                            :data-placement "auto right"
;                            :ref (fn [el]
;                                   ; Store the jQuery value in an atom (safer than reagent/dom-node in mount)
;                                   (some->> el js/$ (reset! dom)))}
;                           (or (:value data) [no-value])]])})))


(defn poppable []
  (let [dom (reagent/atom nil)]
    (reagent/create-class
      {:name "Poppable"
       :component-did-mount (fn []
                              ; Initiate popover functionality
                              (some-> @dom (ocall :popover)))
       :component-did-update (fn []
                               ; When the popover is open...
                               (when (some true? (-> @dom
                                                     (ocall :data "bs.popover")
                                                     (oget :inState)
                                                     js->clj
                                                     vals))
                                 ; ...then re-open it with the new content
                                 (-> @dom (ocall :popover "show"))))
       :reagent-render (fn [props & [remaining]]
                         [:span
                          (merge {:data-trigger "hover"
                                  :data-html true
                                  :data-container "body"
                                  :data-content nil
                                  :data-placement "auto right"
                                  :ref (fn [el] (some->> el js/$ (reset! dom)))} props)
                          remaining])})))

(defn cell []
  (fn [loc {:keys [value id view rows] :as data}]
    [:td
     (if rows
       ; Show outer-join table
       [outer-join-table loc data view]
       ; Show regular cell
       [poppable {:on-mouse-enter (fn [] (dispatch [:main/summarize-item loc data]))
                  :data-trigger "hover"
                  :data-html true
                  :data-container "body"
                  :data-content (dom-server/render-to-static-markup (generate-summary-table @(subscribe [:summary/item-details loc id])))
                  :data-placement "auto right"}
        [:span (or value [no-value])]])]))

(defn table-row [loc row]
  (into [:tr]
        (map-indexed (fn [idx c]
                       ^{:key (str idx (:id c) (:column c))} [cell loc c]))
        row))
