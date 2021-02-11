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

(defn ->html
  "Return the HTML markup of a component"
  [component]
  (dom-server/render-to-static-markup component))

(defn summary-table [{:keys [value column-headers] :as summary}]
  [:table.table.table-striped.table-condensed.table-bordered.summary-table
   (into [:tbody]
         (map-indexed
          (fn [idx column-header]
            (if-let [v (get-in value (dot-split (get (:views summary) idx)))]
              (let [v (if (and (string? v) (> (count v) 200)) (str (clojure.string/join (take 200 v)) "...") v)]
                [:tr
                 [:td (clojure.string/join " > " (drop 1 (clojure.string/split column-header " > ")))]
                 [:td.truncate-text v]])))
          column-headers))])

(defn poppable
  "Wrap child(ren) in a Bootstrap popover, see render function for default values. Ex:
  [:a [poppable {:class some-class :data-content <p>Hello!</p>} [:span banana]]
  Bootstrap related properties can be found at: http://getbootstrap.com/javascript/#popovers-options"
  []
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
                                 :ref (fn [el] (some->> el js/$ (reset! dom)))}
                                props)
                         remaining])})))

(defn outer-join-table-cell [loc]
  (let [pop-el (reagent/atom nil)
        settings (subscribe [:settings/settings loc])]
    (fn [loc {:keys [id value] :as data}]
      (let [{:keys [on-click url vocab]} (get-in @settings [:links])
            item-details @(subscribe [:summary/item-details loc id])
            link (url (merge data (:value item-details) vocab))]
        [:td {:ref (fn [p] (when p (reset! pop-el p)))} ; Store a reference so we can manually kill popups
         [poppable {:on-mouse-enter (fn [] (dispatch [:main/summarize-item loc data]))
                    :data-content (when (seq item-details)
                                    (->html (summary-table item-details)))}
          [:a
           (merge
            (when (and on-click value)
              {:on-click (fn []
                           ; Call the provided on-click
                           (on-click link)
                           ; Side effect!!
                           ; Destroy the popover in case the table is embedded in an SPA
                           ; otherwise it will stick after page routes
                           (-> @pop-el js/$
                               (ocall :find "[data-trigger='hover']")
                               (ocall :popover "destroy")))})
            ;; URL is incomplete until summary has been fetched and
            ;; `:value` key added, so avoid pointing to wrong URL.
            (when (contains? item-details :value)
              {:href link}))
           (or value [no-value])]]]))))

(defn outer-join-table [loc]
  (let [model (subscribe [:assets/model loc])
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
            [:table.table.table-bordered.sub-table
             [:thead
              (into [:tr]
                    (map (fn [th]
                           [:th (last (impath/display-name @model th))])
                         (:view data)))]
             (into [:tbody]
                   (map (fn [rows]
                          (into [:tr]
                                (map (fn [data]
                                       [outer-join-table-cell loc data])
                                     rows)))
                        (:rows data)))]])]))))

(defn select-cell [loc id class]
  (let [is-picked? @(subscribe [:pick-items/is-picked? loc id])]
    [:input {:type "checkbox"
             :on-change (if is-picked?
                          #(dispatch [:pick-items/drop loc id class])
                          #(dispatch [:pick-items/pick loc id class]))
             :checked is-picked?}]))

(defn cell [loc]
  (let [pop-el   (reagent/atom nil)
        settings (subscribe [:settings/settings loc])
        picked (subscribe [:pick-items/picked loc])
        picked-class (subscribe [:pick-items/class loc])]
    (fn [loc {:keys [value id view rows class] :as data}]
      (let [{:keys [on-click url vocab]} (get-in @settings [:links])]
        [:td
         (when (and @picked (if-some [picked-class @picked-class]
                              (= picked-class class)
                              true))
           [select-cell loc id class])
         (if rows
           ; rows means outer-join, so show outer-join table
           [outer-join-table loc data view]
           ; otherwise a regular cell
           (let [item-details @(subscribe [:summary/item-details loc id])
                 link (url (merge data (:value item-details) (get-in @settings [:links :vocab])))]
             [:span {:ref (fn [p] (when p (reset! pop-el p)))} ; Store a reference so we can manually kill popups

              [poppable {:on-mouse-enter (fn [] (dispatch [:main/summarize-item loc data]))
                         :data-content (when (seq item-details)
                                         (->html (summary-table item-details)))}

               [:a (merge
                    {:on-click (fn []
                                 (when (and on-click value)
                                   ; Call the provided on-click
                                   (on-click link)
                                   ; Side effect!!
                                   ; Destroy the popover in case the table is embedded in an SPA
                                   ; otherwise it will stick after page routes
                                   (-> @pop-el js/$
                                       (ocall :find "[data-trigger='hover']")
                                       (ocall :popover "destroy"))))}
                    ;; URL is incomplete until summary has been fetched and
                    ;; `:value` key added, so avoid pointing to wrong URL.
                    (when (contains? item-details :value)
                      {:href link}))
                (or value [no-value])]]]))]))))

(defn table-row [loc row]
  (into [:tr]
        (map-indexed
         (fn [idx c]
            ;(reagent/flush)
           ^{:key idx} [cell loc c])
            ;[:td (:value c)]

         row)))
