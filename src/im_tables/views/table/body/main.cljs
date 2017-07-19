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


(defn outer-join-table []
  (let [model (subscribe [:assets/model])
        open? (reagent/atom false)]
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
                (conj [:thead (into [:tr] (map (fn [th] [:th (last (impath/display-name @model th))]) (:view data)))])
                (conj
                  (into [:tbody]
                        (map (fn [rows]
                               (into [:tr]
                                     (map (fn [{:keys [id value] :as c}]
                                            [:td
                                             [:a [poppable
                                                  {:on-mouse-enter (fn [] (dispatch [:main/summarize-item loc c]))
                                                   :data-content (dom-server/render-to-static-markup
                                                                   (generate-summary-table
                                                                     @(subscribe [:summary/item-details loc id])))}
                                                  [:span (or value [no-value])]]]]))
                                     rows))
                             (:rows data)))))])]))))

(defn cell []
  (fn [loc {:keys [value id view rows] :as data}]
    [:td
     (if rows
       ; rows means outer-join, so show outer-join table
       [outer-join-table loc data view]
       ; otherwise a regular cell
       [poppable {:on-mouse-enter (fn [] (dispatch [:main/summarize-item loc data]))
                  :data-content (dom-server/render-to-static-markup
                                  (generate-summary-table
                                    @(subscribe [:summary/item-details loc id])))}
        [:a (or value [no-value])]])]))

(defn table-row [loc row]
  (into [:tr]
        (map-indexed
          (fn [idx c]
            ^{:key (str idx (:id c) (:column c))} [cell loc c])
          row)))
