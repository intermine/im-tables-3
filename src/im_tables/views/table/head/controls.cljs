(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.graphs.histogram :as histogram]
            [im-tables.views.common :refer [no-value]]
            [goog.i18n.NumberFormat.Format]
            [imcljs.path :as path]
            [clojure.string :as string]
            [oops.core :refer [oget ocall ocall! oapply oget+]])
  (:import
   (goog.i18n NumberFormat)
   (goog.i18n.NumberFormat Format)))

(def nff
  (NumberFormat. Format/DECIMAL))

(defn- nf
  [num]
  (.format nff (str num)))

(defn filter-input []
  (fn [loc view val]
    [:div.inline-filter [:i.fa.fa-filter]
     [:input.form-control
      {:type "text"
       :value val
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
     {:value (if value value "=")
      :on-change (fn [e] (on-change {:op (.. e -target -value)}))}
     [:option {:value ">"} ">"]
     [:option {:value ">="} ">="]
     [:option {:value "<"} "<"]
     [:option {:value "<="} "<="]
     [:option {:value "="} "="]
     [:option {:value "!="} "!="]
     [:option {:value "LIKE"} "LIKE"]
     [:option {:value "NOT LIKE"} "NOT LIKE"]
     [:option {:value "CONTAINS"} "CONTAINS"]
     [:option {:value "ONE OF"} "ONE OF"]
     [:option {:value "NONE OF"} "NONE OF"]]))

(defn constraint-text []
  (fn [{:keys [value on-change]}]
    [:input.form-control {:type "text"
                          :on-change (fn [e] (on-change {:value (.. e -target -value)}))
                          :value value}]))

(defn blank-constraint [loc path state]
  (fn [loc path]
    (let [submit-constraint (fn [] (dispatch
                                    [:filters/add-constraint loc @state]
                                    (reset! state {:path path :op "=" :value nil})))]
      [:div.imtable-constraint
       [:div.constraint-operator
        [constraint-dropdown
         {:value (:op @state)
          :on-change (fn [v] (swap! state assoc :op (:op v)))}]]
       [:div.constraint-input [:input.form-control
                               {:type "text"
                                :value (:value @state)
                                :on-change (fn [e] (swap! state assoc :value (.. e -target -value)))
                                ;:on-blur (fn [e] (when (not (clojure.string/blank? (.. e -target -value)))
                                ;                   (submit-constraint)))
                                :on-key-press
                                (fn [e]
                                  (let [keycode (.-charCode e)
                                        input (.. e -target -value)]
                                    ;; submit when pressing enter & not blank.
                                    (when (and (= keycode 13) (not (clojure.string/blank? input)))
                                      (submit-constraint))))}]]])))

(defn constraint []
  (fn [loc {:keys [path op value values code] :as const}]
    (letfn [(on-change [new-value] (dispatch [:filters/update-constraint loc (merge const new-value)]))]
      [:div.imtable-constraint
       [:div.constraint-operator
        [constraint-dropdown {:value op
                              :on-change on-change}]]
       [:div.constraint-input
        [constraint-text {:value (or value values)
                          :on-change on-change}]]
       [:button.btn.btn-danger
        {:on-click (fn [] (dispatch [:filters/remove-constraint loc const]))
         :type "button"} [:i.fa.fa-times]]])))

(defn filter-view [loc view blank-constraint-atom]
  (let [response (subscribe [:selection/response loc view])
        selections (subscribe [:selection/selections loc view])
        query (subscribe [:main/temp-query loc view])]
    (fn [loc view]
      (let [active-filters (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query)))
            dropdown (reagent/current-component)]
        [:form.form.filter-view {:style {:padding "5px"}
                                 :on-submit (fn [e]
                                              (ocall e "preventDefault")
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
             :type "button"} "Add More"]]

          [:div.btn-group
           [:input.btn.btn-primary.pull-right
            {:type "submit"
             :on-click (fn []
                         (when (not (clojure.string/blank? (:value @blank-constraint-atom)))
                           (dispatch
                            [:filters/add-constraint loc @blank-constraint-atom]
                            (reset! blank-constraint-atom {:path view :op "=" :value nil}))))
             ; don't put :data-toggle "dropdown" in here, it stops
             ; the form submitting.... silently. Nice.
             :value "Apply"}]]]]))))

(defn too-many-values []
  (fn []
    [:div.alert.alert-warning
     [:div
      {:style {:padding "10px"}}
      [:h3 "Column summary not available"]
      [:p "Summaries can only be made on columns with 1,000 values or less."]]]))

(def clj-min min)
(def clj-max max)

(defn numerical-column-summary []
  (let []
    (fn [loc view results trimmer]
      (let [model (subscribe [:assets/model loc])
            {:keys [min max average stdev]} (first results)
            close-fn (partial force-close (reagent/current-component))
            display-name (str (string/join " " (take-last 2 (string/split (path/friendly @model view) " > "))) "s")]
        [:form.form.column-summary
         [:h4 (str "Showing numerical distribution for " (count results) " " display-name)]
         [histogram/numerical-histogram results @trimmer]
         [:div.main-view
          [:div.numerical-content-wrapper
           [:table.table.table-condensed
            [:thead
             [:tr [:th "Min"] [:th "Max"] [:th "Average"] [:th "Std Deviation"]]]
            [:tbody
             [:tr [:td (nf min)] [:td (nf max)] [:td (nf average)] [:td (nf stdev)]]]]
           [:div
            [:label "Trim from " [:input {:type "text"
                                          :value (or (:from @trimmer) min)
                                          :on-change (fn [e]
                                                       (swap! trimmer assoc :from (oget e :target :value)))}]]
            [:input {:type "range"
                     :min min
                     :value (or (:from @trimmer) min)
                     :max max
                     :on-change (fn [e] (swap! trimmer assoc :from
                                               (let [new-value (js/parseInt (clj-min (or (:to @trimmer) max) (oget e :target :value)))]
                                                 (if (= new-value min) nil new-value))))}]
            [:label "Trim to " [:input {:type "text"
                                        :value (or (:to @trimmer) max)
                                        :placeholder max
                                        :on-change (fn [e]
                                                     (swap! trimmer assoc :to (oget e :target :value)))}]]
            [:input {:type "range"
                     :min min
                     :value (or (:to @trimmer) max)
                     :max max
                     :on-change (fn [e] (swap! trimmer assoc :to
                                               (let [new-value (js/parseInt (clj-max (or (:from @trimmer) min) (oget e :target :value)))]
                                                 (if (= new-value max) nil new-value))))}]]]
          [:div.btn-toolbar.column-summary-toolbar
           [:button.btn.btn-primary
            {:type "button"
             :on-click (fn []
                         (dispatch [:main/apply-numerical-filter loc view @trimmer])
                         (close-fn)
                         (reset! trimmer {}))}
            [:i.fa.fa-filter]
            (str " Filter")]]]]))))

(defn path->displaynames
  "Takes a path as the `view` argument and returns the corresponding vector of
  display names. Will prioritise the displayName of the referencedType class
  (instead of the displayName in the references/collections/attributes map).
  This makes a difference with eg. `Gene.dataSets.name`, where
  `classes.Gene.collections.dataSets.displayName` is `Data Sets` while
  `classes.DataSet.displayName` is `Data Set`."
  [model view]
  (let [[head & tail] (path/split-path view)]
    (loop [names []
           paths tail
           class (get-in model [:classes head])]
      (let [new-names (conj names (:displayName class))]
        (if (seq paths)
          (recur new-names
                 (next paths)
                 (let [subclasses (apply merge
                                         ((juxt :references :collections :attributes) class))
                       subclass   (subclasses (first paths))]
                   (if-let [reference (:referencedType subclass)]
                     (get-in model [:classes (keyword reference)])
                     subclass)))
          new-names)))))

(defn pluralise
  "Takes a string and adds an 's' to the end if not present."
  [s]
  (cond-> s
    (not (string/ends-with? s "s"))
    (str "s")))

(defn hr-name
  "Takes a view path and returns a human-readable name string."
  [model view]
  (->> (path->displaynames model view)
       (take-last 2) ;; Last two names of the path are most descriptive.
       (string/join " ")
       pluralise))

(defn column-summary-title [loc view response]
  (let [model @(subscribe [:assets/model loc])
        {:keys [results uniqueValues]} response
        human-name (hr-name model view)]
    [:h4.title
     (if (< (count results) uniqueValues)
       (str "Showing " (nf (count results)) " of "
            (nf uniqueValues) " " human-name)
       (str (nf uniqueValues) " " human-name))]))

(defn column-summary [loc view local-state]
  (let [response (subscribe [:selection/response loc view])
        selections (subscribe [:selection/selections loc view])
        text-filter (subscribe [:selection/text-filter loc view])]
    (reagent/create-class
     {:component-will-mount
      (fn [])
      :component-will-update
      (fn [])
      :reagent-render
      (fn [loc view]
        (let [close-fn (partial force-close (reagent/current-component))]
          (if (false? @response)
            [too-many-values]
            (if (contains? (first (:results @response)) :min)
              [numerical-column-summary loc view (:results @response) local-state]
              [:form.form.column-summary
               [:div.main-view
                [column-summary-title loc view @response]
                [histogram/main (:results @response)]
                [filter-input loc view @text-filter]
                [:table.table.table-striped.table-condensed
                 [:thead [:tr [:th
                               (if (empty? @selections)
                                 [:span {:title "Select all"
                                         :on-click (fn [] (dispatch [:select/select-all loc view]))} [:i.fa.fa-check-square-o]]
                                 [:span {:title "Deselect all"
                                         :on-click (fn [] (dispatch [:select/clear-selection loc view]))} [:i.fa.fa-square-o]])] [:th "Item"] [:th "Count"]]]
                 (into [:tbody]
                       (->> (filter (partial has-text? @text-filter) (:results @response))
                            (map (fn [{:keys [count item]}]
                                   [:tr.hoverable
                                    {:on-click (fn [e] (dispatch [:select/toggle-selection loc view item]))}
                                    [:td
                                     [:input
                                      {:on-change (fn [])
                                       :checked (contains? @selections item)
                                       :type "checkbox"}]]
                                    [:td (if item item [no-value])]
                                    [:td
                                     [:div count]]]))))]]
               [:div.btn-toolbar.column-summary-toolbar
                [:button.btn.btn-primary
                 {:type "button"
                  :on-click (fn []
                              (dispatch [:main/apply-summary-filter loc view])
                              (close-fn))}
                 [:i.fa.fa-filter]
                 (str " Filter")]]]))))})))

(defn filter-dropdown-menu [loc view idx col-count]
  (let [query (subscribe [:main/query loc view])
        blank-constraint-atom (reagent/atom {:path view :op "=" :value nil})]
    (fn [loc view idx col-count right?]
      (let [active-filters? (not-empty (filter (partial constraint-has-path? view) (:where @query)))]
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
          :ref (fn [e]
                 ; *Try* to save the changes every time the dropdown is closed, even by just clicking off it.
                 ; This means a user can remove a handful of filters without having to click Apply.
                 ; The event will do a diff to make sure something has actually changed before rerunning the query
                 (some-> e js/$ (ocall :on "hide.bs.dropdown" (fn [] (dispatch [:filters/save-changes loc])))))}
         [:i.fa.fa-filter.dropdown-toggle.filter-icon
          {:on-click (fn [] (dispatch [:main/set-temp-query loc]))
           :data-toggle "dropdown"
           :class (cond active-filters? "active-filter")
           :title (str "Filter " view " column")}]
         ; Crudely try to draw the dropdown near the middle of the page
         [:div.dropdown-menu
          {:class (when right? "dropdown-right")} [filter-view loc view blank-constraint-atom]]]))))

(defn obj->clj [obj]
  (reduce (fn [total next-key]
            (assoc total (keyword next-key) (oget+ obj next-key))) {} (js-keys obj)))

(defn align-right? [dom-node]
  (let [{left :left} (obj->clj (ocall dom-node :getBoundingClientRect))
        screen-width (oget js/window :innerWidth)]
    (> left (/ screen-width 2))))

(defn on
  "For use with `:ref` attribute on elements to easily define jquery listeners.
  Uses `some->` as the event isn't guaranteed to hold a value. Returns the event
  so you can chain multiple `on` calls by wrapping them in `comp`. Example:
      [:span.dropdown
       {:ref (comp
               (on \"hide.bs.dropdown\"
                   #(js/alert \"I'm closing!\"))
               (on \"show.bs.dropdown\"
                   #(js/alert \"I'm opening!\")))}]"
  [trigger callback]
  (fn [event]
    (some-> event
            js/$
            (ocall :off trigger)
            (ocall :on trigger callback))
    event))

(defn toolbar []
  (let [right? (reagent/atom false)]
    (fn [loc view idx col-count]
      (let [query (subscribe [:main/temp-query loc view])
            response (subscribe [:selection/response loc view])
            active-filters? (seq (map (fn [c] [constraint loc c]) (filter (partial constraint-has-path? view) (:where @query))))
            local-state (reagent/atom {})]
        [:div.summary-toolbar
         {:ref (fn [e]
                 (when e (reset! right? (align-right? e))))}
         [:i.fa.fa-sort.sort-icon
          {:on-click (fn [] (dispatch [:main/sort-by loc view]))
           :title (str "Sort " view " column")}]
         [:i.fa.fa-times.remove-icon
          {:on-click (fn [] (dispatch [:main/remove-view loc view]))
           :title (str "Remove " view " column")}]
         [filter-dropdown-menu loc view idx col-count @right?]
         [:span.dropdown
          ;; Bind an event to clear the selected items when the dropdown
          ;; closes. Why don't we just avoid state all together and pick up the
          ;; checkbox values when the user clicks "Filter"? Because we still
          ;; want to know what's selected (for instance, highlighting the
          ;; histogram).
          {:ref (comp
                  (on "hide.bs.dropdown"
                      (fn []
                        (reset! local-state {})
                        (dispatch [:select/clear-selection loc view])))
                  (on "show.bs.dropdown"
                      #(when (nil? @response)
                         (dispatch [:main/summarize-column loc view]))))}
          [:i.fa.fa-bar-chart.dropdown-toggle {:data-toggle "dropdown"}]
          [:div.dropdown-menu
           {:title (str "Summarise " view " column")
            :class (when @right? "dropdown-right")}
           [column-summary loc view local-state]]]]))))
