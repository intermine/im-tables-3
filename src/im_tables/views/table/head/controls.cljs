(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.graphs.histogram :as histogram]
            [oops.core :refer [oget ocall ocall! oapply]]))


(defn filter-input []
  (fn [loc view val]
    [:div.inline-filter [:i.fa.fa-filter]
     [:input.form-control
     {:type      "text"
      :value     val
      :placeholder "Search for a value..."
      :on-change (fn [e]
                   (dispatch [:select/set-text-filter
                              loc
                              view
                              (oget e :target :value)]))}]]))

(defn force-close
  "Force a dropdown to close "
  [component]
  (-> (js/$ (reagent/dom-node component))
      (ocall! "closest" ".dropdown-menu")
      (ocall! "parent")
      (ocall! "removeClass" "open")))


(defn has-text?
  "Return true if a label contains a string"
  [needle haystack]
  (if needle
    (if-let [text-to-search (:item haystack)]
      (re-find (re-pattern (str "(?i)" needle)) (:item haystack))
      false)
    true))

(defn constraint-has-path? [view constraint]
  (= view (:path constraint)))


(defn constraint-dropdown []
  (fn [{:keys [value on-change]}]
    [:select.form-control
     {:value     (if value value "=")
      :on-change (fn [e] (on-change {:op (.. e -target -value)}))}
     [:option {:value ">"} "greater than"]
     [:option {:value "<"} "less than"]
     [:option {:value "="} "equal to"]
     [:option {:value "CONTAINS"} "contains"]
     [:option {:value "ONE OF"} "one of"]]))

(defn constraint-text []
  (fn [{:keys [value on-change]}]
    [:input.form-control {:type      "text"
                          :on-change (fn [e] (on-change {:value (.. e -target -value)}))
                          :value     value}]))

(defn blank-constraint [loc path]
  (let [state (reagent/atom {:path path :op "=" :value nil})]
    (fn [loc path]
      (let [submit-constraint (fn [] (dispatch
                          [:filters/add-constraint loc @state]
                          (reset! state {:path path :op "=" :value nil})))]
      [:div.container-fluid
       [:div.row
        [:div.col-xs-4
         [constraint-dropdown
          {:value     (:op @state)
           :on-change (fn [v] (swap! state assoc :op (:op v)))}]]
        [:div.col-xs-6
         [:input.form-control
          {:type      "text"
           :value     (:value @state)
           :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))
           :on-blur (fn [e] (when (not (clojure.string/blank? (.. e -target -value)))
                              (submit-constraint)))
           :on-key-press
             (fn [e]
               (let [keycode (.-charCode e)
                     input   (.. e -target -value)]
                 ;; submit when pressing enter & not blank.
                 (when (and (= keycode 13) (not (clojure.string/blank? input)))
                  (submit-constraint)
                  )))
            }]]
        [:div.col-xs-2
         [:button.btn.btn-success
          {:on-click (fn [] (dispatch
                              [:filters/add-constraint loc @state]
                              (reset! state {:path path :op "=" :value nil})))
           :type     "button"} [:i.fa.fa-plus]]]]]))))

(defn constraint []
  (fn [loc {:keys [path op value values code] :as const}]
    (letfn [(on-change [new-value] (dispatch [:filters/update-constraint loc (merge const new-value)]))]
      [:div.container-fluid
       [:div.row
        [:div.col-xs-4
         [constraint-dropdown {:value     op
                               :on-change on-change}]]
        [:div.col-xs-6
         [constraint-text {:value     (or value values)
                           :on-change on-change}]]
        [:div.col-xs-2
         [:button.btn.btn-danger
          {:on-click (fn [] (dispatch [:filters/remove-constraint loc const]))
           :type     "button"} [:i.fa.fa-times]]]]])))


(defn filter-view [loc view]
  (let [response   (subscribe [:selection/response loc view])
        selections (subscribe [:selection/selections loc view])
        query      (subscribe [:main/temp-query loc view])]
    (fn [loc view]
      (let [active-filters (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query)))
            dropdown (reagent/current-component)]
      [:form.form.filter-view {
       :on-submit (fn [e]
         (ocall e "preventDefault")
         (force-close dropdown)
         (dispatch [:filters/save-changes loc])
       )}
       [:div.alert.alert-success
          (if (seq active-filters)
            (into [:div [:h4 "Active filters:"] ] active-filters)
            [:h4 "No active filters"])]
       [:div.alert.alert-default
         [:h4 "Add a new filter:"]
         [blank-constraint loc view]]
       [:div.container-fluid
        [:div.btn-toolbar
         [:button.btn.btn-default
          {:type        "button"
           :data-toggle "dropdown"} "Cancel"]
         [:input.btn.btn-primary.pull-right
          {:type        "submit"
           ; don't put :data-toggle "dropdown" in here, it stops
           ; the form submitting.... silently. Nice.
           :value  "Apply"
           }]]]]))))

(defn column-summary [loc view]
  (let [response    (subscribe [:selection/response loc view])
        selections  (subscribe [:selection/selections loc view])
        text-filter (subscribe [:selection/text-filter loc view])]
    (reagent/create-class
      {:component-will-mount
       (fn [])
       :component-will-update
       (fn [])
       :reagent-render
       (fn [loc view]
         (let [close-fn (partial force-close (reagent/current-component))]
           [:form.form.column-summary
            [histogram/main (:results @response)]
            [filter-input loc view @text-filter]
             [:table.table.table-striped.table-condensed
              [:thead [:tr [:th] [:th "Item"] [:th "Count"]]]
              (into [:tbody]
                    (->> (filter (partial has-text? @text-filter) (:results @response))
                         (map (fn [{:keys [count item]}]
                                [:tr.hoverable
                                 {:on-click (fn [e] (dispatch [:select/toggle-selection loc view item]))}
                                 [:td
                                  [:input
                                         {:on-change (fn [])
                                          :checked   (contains? @selections item)
                                          :type      "checkbox"}]]
                                 [:td (if item item [:i.fa.fa-ban.mostly-transparent])]
                                 [:td
                                  [:div count]]]))))]
            [:div.btn-toolbar
             [:button.btn.btn-primary
              {:type     "button"
               :on-click (fn []
                           (dispatch [:main/apply-summary-filter loc view])
                           (close-fn))}
              [:span
               [:i.fa.fa-filter]
               (str " Filter (" (count (keys @selections)) ")")]]
             (if (empty? @selections)
               [:button.btn.btn-default
                {:type     "button"
                 :on-click (fn [] (dispatch [:select/select-all loc view]))}
                [:span [:i.fa.fa-check-square-o] " All"]]
               [:button.btn.btn-default
                {:type     "button"
                 :disabled (empty? @selections)
                 :on-click (fn [] (dispatch [:select/clear-selection loc view]))}
                [:span [:i.fa.fa-square-o] " Clear"]])]]))})))

(defn toolbar []
  (fn [loc view idx col-count]
    (let [query (subscribe [:main/temp-query loc view])
          active-filters? (seq (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query))))
          direction (if (> idx (/ col-count 2)) "dropdown-right" "dropdown-left")]
    [:div.summary-toolbar
     [:i.fa.fa-sort.sort-icon
      {:on-click (fn [] (dispatch [:main/sort-by loc view]))
       :title (str "Sort " view " column")}]
     [:i.fa.fa-times.remove-icon
      {:on-click (fn [] (dispatch [:main/remove-view loc view]))
       :title (str "Remove " view " column")}]
     [:span.dropdown
      [:i.fa.fa-filter.dropdown-toggle.filter-icon
       {:on-click    (fn [] (dispatch [:main/set-temp-query loc]))
        :data-toggle "dropdown"
        :class (cond active-filters? "active-filter")
        :title (str "Filter " view " column")}]
      [:div.dropdown-menu {:class direction} [filter-view loc view]]]
     [:span.dropdown
      [:i.fa.fa-bar-chart.dropdown-toggle {:data-toggle "dropdown"}]
      [:div.dropdown-menu
       {:title (str "Summarise " view " column")
        :class direction}
       [column-summary loc view]]]])))
