(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.graphs.histogram :as histogram]
            [im-tables.views.common :refer [no-value]]
            [imcljs.path :as im-path]
            [clojure.string :as string]
            [oops.core :refer [oget ocall ocall! oapply]]))


(defn filter-input []
  (fn [loc view val]
    [:div.inline-filter [:i.fa.fa-filter]
     [:input.form-control
      {:type        "text"
       :value       val
       :placeholder "Search for a value..."
       :on-change   (fn [e]
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
     [:option {:value "!="} "not equal to"]
     [:option {:value "LIKE"} "like"]
     [:option {:value "NOT LIKE"} "not like"]
     [:option {:value "CONTAINS"} "contains"]
     [:option {:value "ONE OF"} "one of"]
     [:option {:value "NONE OF"} "none of"]]))

(defn constraint-text []
  (fn [{:keys [value on-change]}]
    [:input.form-control {:type      "text"
                          :on-change (fn [e] (on-change {:value (.. e -target -value)}))
                          :value     value}]))

(defn blank-constraint [loc path state]
  (fn [loc path]
    (let [submit-constraint (fn [] (dispatch
                                     [:filters/add-constraint loc @state]
                                     (reset! state {:path path :op "=" :value nil})))]
      [:div.imtable-constraint
       [:div.constraint-operator
        [constraint-dropdown
         {:value     (:op @state)
          :on-change (fn [v] (swap! state assoc :op (:op v)))}]]
       [:div.constraint-input [:input.form-control
                               {:type      "text"
                                :value     (:value @state)
                                :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))
                                ;:on-blur (fn [e] (when (not (clojure.string/blank? (.. e -target -value)))
                                ;                   (submit-constraint)))
                                :on-key-press
                                           (fn [e]
                                             (let [keycode (.-charCode e)
                                                   input   (.. e -target -value)]
                                               ;; submit when pressing enter & not blank.
                                               (when (and (= keycode 13) (not (clojure.string/blank? input)))
                                                 (submit-constraint)
                                                 )))}]]
       ])))

(defn constraint []
  (fn [loc {:keys [path op value values code] :as const}]
    (letfn [(on-change [new-value] (dispatch [:filters/update-constraint loc (merge const new-value)]))]
      [:div.imtable-constraint
       [:div.constraint-operator
        [constraint-dropdown {:value     op
                              :on-change on-change}]]
       [:div.constraint-input
        [constraint-text {:value     (or value values)
                          :on-change on-change}]]
       [:button.btn.btn-danger
        {:on-click (fn [] (dispatch [:filters/remove-constraint loc const]))
         :type     "button"} [:i.fa.fa-times]]])))


(defn filter-view [loc view blank-constraint-atom selected-subview]
  (let [response   (subscribe [:selection/response loc view])
        selections (subscribe [:selection/selections loc view])
        query      (subscribe [:main/temp-query loc view])]
    (fn [loc view]
      (let [active-filters (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query)))
            dropdown       (reagent/current-component)]
        [:form.form.filter-view {:style     {:padding "5px"}
                                 :on-click (fn [e]
                                             (ocall e :stopPropagation))
                                 :on-submit (fn [e]
                                              (ocall e "preventDefault")
                                              (reset! selected-subview nil)
                                              (force-close dropdown)
                                              (dispatch [:filters/save-changes loc]))}
         [:div.alert.alert-success
          (if (seq active-filters)
            (into [:div [:h4 "Active filters:"]] active-filters)
            [:h4 "No active filters"])]
         [:div.alert.alert-default
          [:h4 "Add a new filter:"]
          ; Note that we're storing the blank constraint's
          [blank-constraint loc view blank-constraint-atom]]
         [:div.btn-toolbar.pull-right
          [:div.btn-group
           [:button.btn.btn-default
            {:on-click (fn [] (dispatch
                                [:filters/add-constraint loc @blank-constraint-atom]
                                (reset! blank-constraint-atom {:path view :op "=" :value nil})))
             :type     "button"} "Add More"]]

          [:div.btn-group
           [:input.btn.btn-primary.pull-right
            {:type     "submit"
             :on-click (fn []
                         (when (not (clojure.string/blank? (:value @blank-constraint-atom)))
                           (dispatch
                             [:filters/add-constraint loc @blank-constraint-atom]
                             (reset! blank-constraint-atom {:path view :op "=" :value nil}))))
             ; don't put :data-toggle "dropdown" in here, it stops
             ; the form submitting.... silently. Nice.
             :value    "Apply"
             }]]]]))))

(defn too-many-values []
  (fn []
    [:div.alert.alert-warning
     [:div
      {:style {:padding "10px"}}
      [:h3 "Column summary not available"]
      [:p "Summaries can only be made on columns with 1,000 values or less."]]]))

(defn column-summary [loc view selected-subview]
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
         (let [local-state (reagent/atom [])
               close-fn    (partial force-close (reagent/current-component))]
           (if (false? @response)
             [too-many-values]
             [:form.form.column-summary
              [:div.main-view
               [histogram/main (:results @response)]
               [filter-input loc view @text-filter]
               [:table.table.table-striped.table-condensed
                [:thead [:tr [:th
                              (if (empty? @selections)
                                [:span {:title    "Select all"
                                        :on-click (fn [] (dispatch [:select/select-all loc view]))} [:i.fa.fa-check-square-o]]
                                [:span {:title    "Deselect all"
                                        :on-click (fn [] (dispatch [:select/clear-selection loc view]))} [:i.fa.fa-square-o]])
                              ] [:th "Item"] [:th "Count"]]]
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
                                   [:td (if item item [no-value])]
                                   [:td
                                    [:div count]]]))))]]
              [:div.btn-toolbar.column-summary-toolbar
               [:button.btn.btn-primary
                {:type     "button"
                 :on-click (fn []
                             (dispatch [:main/apply-summary-filter loc view])
                             (reset! selected-subview nil)
                             (close-fn))}
                [:i.fa.fa-filter]
                (str " Filter")]]])))})))


(defn filter-dropdown-menu [loc view idx col-count]
  (let [query                 (subscribe [:main/temp-query loc view])
        active-filters?       (seq (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query))))
        blank-constraint-atom (reagent/atom {:path view :op "=" :value nil})
        selected-subview      (reagent/atom nil)
        model                 (subscribe [:assets/model loc])]
    (fn [loc view idx col-count subviews]
      [:span.dropdown
       ; Bootstrap and ReactJS don't always mix well. Components that make up dropdown menus are only
       ; mounted (reactjs) once and then their visibility is toggled (bootstrap). These means any local state
       ; in the dropdown menu does NOT get reset when the dropdown menu disappears because its never really unmounts.
       ; This was causing the new constraint textbox to retain its value if a user entered something but changed their
       ; mind and closed the dropdown. Reopening it would show what they previously entered. Making it look like it had been applied.;
       ; To fix this we've create the black constraint local atom all the way up here at the dropdown level so that we can reset
       ; it manually, and then we pass the atom down to the black constraint component. Lame.
       {:on-click (fn []
                    ; Reset the blank constraint atom when the dropdown is opened
                    (reset! blank-constraint-atom {:path view :op "=" :value nil}))
        :ref      (fn [e]
                    ; *Try* to save the changes every time the dropdown is closed, even by just clicking off it.
                    ; This means a user can remove a handful of filters without having to click Apply.
                    ; The event will do a diff to make sure something has actually changed before rerunning the query
                    (some-> e js/$ (ocall :on "hide.bs.dropdown" (fn []
                                                                   (dispatch [:filters/save-changes loc])
                                                                   (reset! selected-subview nil)))))}
       [:i.fa.fa-filter.dropdown-toggle.filter-icon
        {:on-click    (fn [] (dispatch [:main/set-temp-query loc]))
         :data-toggle "dropdown"
         :class       (cond active-filters? "active-filter")
         :title       (str "Filter " view " column")}]
       ; Crudely try to draw the dropdown near the middle of the page
       [:div.dropdown-menu {:class (if (> idx (/ col-count 2)) "dropdown-right" "dropdown-left")}
        (if-not subviews
          [filter-view loc view blank-constraint-atom selected-subview]
          (if-not @selected-subview
            [:form.form
             {:style {:padding "10px"}}
             [:p "Selected a nested column to filter"]
             (into [:ul.list-unstyled]
                   (map (fn [subview]
                          [:li {:on-click
                                (fn [e]
                                  (ocall e :stopPropagation)
                                  (reset! selected-subview subview)
                                  (reset! blank-constraint-atom {:path subview :op "=" :value nil}))}
                           [:a (string/join " > " (rest (im-path/display-name @model subview)))]]) subviews))]
            [:div
             [filter-view loc @selected-subview blank-constraint-atom selected-subview]]
            ))






        ;[filter-view loc view blank-constraint-atom]

        ]])))

(defn toolbar []
  (let [selected-subview (reagent/atom nil)]
    (fn [loc view idx col-count subviews]
      (let [query           (subscribe [:main/temp-query loc view])
            active-filters? (seq (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query))))
            direction       (if (> idx (/ col-count 2)) "dropdown-right" "dropdown-left")
            model           (subscribe [:assets/model loc])]
        [:div.summary-toolbar
         [:i.fa.fa-sort.sort-icon
          {:on-click (fn [] (dispatch [:main/sort-by loc view]))
           :title    (str "Sort " view " column")}]
         [:i.fa.fa-times.remove-icon
          {:on-click (fn []
                       (dispatch [:main/remove-view loc (if subviews (im-path/trim-to-last-class @model (first subviews)) view)]))
           :title    (str "Remove " view " column")}]


         [filter-dropdown-menu loc view idx col-count subviews]

         [:span.dropdown
          {:ref (fn [e]
                  ; Bind an event to clear the selected items when the dropdown closes.
                  ; Why don't we just avoid state all together and pick up the checkbox values
                  ; when the user clicks "Filter"? Because we still want to know what's selected
                  ; (for instance, highlighting the histogram).
                  ; Use some-> because e isn't guaranteed to hold a value
                  (some-> e js/$ (ocall :on "hide.bs.dropdown" (fn []
                                                                 (dispatch [:select/clear-selection loc (or @selected-subview view)])
                                                                 ; This view might be part of a join and therefore
                                                                 ; nested in the column, so clear the local state
                                                                 ; just in case
                                                                 (reset! selected-subview nil)))))}
          [:i.fa.fa-bar-chart.dropdown-toggle {:data-toggle "dropdown"}]
          [:div.dropdown-menu
           {:title (str "Summarise " view " column")
            :class direction}
           (if-not subviews
             [column-summary loc view selected-subview]
             (if-not @selected-subview
               [:form.form
                {:style {:padding "10px"}}
                [:p "Selected a nested column to summarize"]
                (into [:ul.list-unstyled]
                      (map (fn [subview]
                             [:li {:on-click
                                   (fn []
                                     (reset! selected-subview subview))}
                              [:a (string/join " > " (rest (im-path/display-name @model subview)))]]) subviews))]
               [column-summary loc @selected-subview selected-subview]
               ))]]]))))
