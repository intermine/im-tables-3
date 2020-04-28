(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [im-tables.views.graphs.histogram :as histogram]
            [im-tables.views.common :refer [no-value]]
            [imcljs.path :as path]
            [clojure.string :as string]
            [clojure.set :as set]
            [oops.core :refer [oget ocall ocall! oget+]]
            [im-tables.utils :refer [on-event pretty-number display-name]]
            [cljs-time.coerce :as time-coerce]
            [cljs-time.format :as time-format]
            [goog.dom :as gdom]
            [goog.style :as gstyle]))

(def css-transition-group
  (reagent/adapt-react-class js/ReactTransitionGroup.CSSTransitionGroup))

(def operators [{:op "LOOKUP"
                 :label "Lookup"
                 :applies-to #{nil}}
                {:op "IN"
                 :label "In list"
                 :applies-to #{nil}}
                {:op "NOT IN"
                 :label "Not in list"
                 :applies-to #{nil}}
                {:op "="
                 :label "="
                 :applies-to #{"java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}}
                {:op "!="
                 :label "!="
                 :applies-to #{"java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}}
                {:op "<"
                 :label "<"
                 :applies-to #{"java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}}
                {:op "<="
                 :label "<="
                 :applies-to #{"java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}}
                {:op ">"
                 :label ">"
                 :applies-to #{"java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}}
                {:op ">="
                 :label ">="
                 :applies-to #{"java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}}
                {:op "CONTAINS"
                 :label "Contains"
                 :applies-to #{"java.lang.String"}}
                {:op "LIKE"
                 :label "Like"
                 :applies-to #{"java.lang.String"}}
                {:op "NOT LIKE"
                 :label "Not like"
                 :applies-to #{"java.lang.String"}}
                {:op "ONE OF"
                 :label "One of"
                 :applies-to #{"java.lang.String"}
                 :multiple-values? true}
                {:op "NONE OF"
                 :label "None of"
                 :applies-to #{"java.lang.String"}
                 :multiple-values? true}
                {:op "IS NULL"
                 :label "Null"
                 :applies-to #{"java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}
                 :no-value? true}
                {:op "IS NOT NULL"
                 :label "Not null"
                 :applies-to #{"java.lang.String" "java.lang.Boolean" "java.lang.Integer" "java.lang.Double" "java.lang.Float" "java.util.Date"}
                 :no-value? true}])

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
  (-> (js/$ (dom/dom-node component))
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

(defn constraint-dropdown [{:keys [value on-change data-type]}]
  (into [:select.form-control
         {:value (or value "=")
          :on-change (fn [e] (on-change {:op (.. e -target -value)}))}]
        (for [{:keys [op label applies-to]} operators
              :when (contains? applies-to data-type)]
          [:option {:value op} label])))

;; The following input components and code are duplicated from BlueGenes.
;; If we wish to continue using im-tables-3 in the future, we should consider
;; moving these into a library of common components.

(def fmt "yyyy-MM-dd")
(def date-fmt (time-format/formatter fmt))

(defn read-day-change
  "Convert DayPicker input to the string we use as constraint."
  [date _mods picker]
  (let [input (-> (ocall picker :getInput)
                  (oget :value))]
    ;; `date` can be nil if it's not a valid date. We use the raw input text in
    ;; that case, to accomodate alien calendars.
    (or (some->> date
                 (time-coerce/from-date)
                 (time-format/unparse date-fmt))
        input)))

(defn date-constraint-input
  "Wraps `cljsjs/react-day-picker` for use as constraint input for selecting dates."
  [{:keys [value on-change on-blur]}]
  [:> js/DayPicker.Input
   {:inputProps {:class "form-control"}
    :value (or value "")
    :placeholder "YYYY-MM-DD"
    :formatDate (fn [date _ _]
                  (if (instance? js/Date date)
                    (->> date (time-coerce/from-date) (time-format/unparse date-fmt))
                    ""))
    :parseDate (fn [s _ _]
                 (when (and (string? s)
                            (= (count s) (count fmt)))
                   ;; Invalid dates like "2020-03-33" causes cljs-time
                   ;; to throw an error. We don't care and return nil.
                   (try
                     (some->> s (time-format/parse date-fmt) (time-coerce/to-date))
                     (catch js/Error _
                       nil))))
    :onDayChange (comp on-change read-day-change)
    :onDayPickerHide #(on-blur value)}])

(defn select-placeholder
  [model path]
  (->> (string/split (path/friendly model path) " > ")
       (take-last 2)
       (string/join " > ")
       (str "Choose ")))

(defn select-constraint-input
  "Wraps `cljsjs/react-select` for use as constraint input for selecting
  one value out of `possible-values`."
  [{:keys [model path value on-blur possible-values disabled]}]
  [:> js/Select
   {:className "constraint-select"
    :classNamePrefix "constraint-select"
    :placeholder (select-placeholder model path)
    :isDisabled disabled
    ;; Leaving the line below as it can be useful in the future.
    ; :isLoading (seq? possible-values)
    :onChange (fn [value]
                (on-blur (oget value :value)))
    :value (when (not-empty value) {:value value :label value})
    :options (map (fn [v] {:value v :label v}) (remove nil? possible-values))}])

(defn select-multiple-constraint-input
  "Wraps `cljsjs/react-select` for use as constraint input for selecting
  multiple values out of `possible-values`."
  [{:keys [model path value on-blur possible-values disabled]}]
  [:> js/Select
   {:className "constraint-select"
    :classNamePrefix "constraint-select"
    :placeholder (select-placeholder model path)
    :isMulti true
    :isDisabled disabled
    ;; Leaving the line below as it can be useful in the future.
    ; :isLoading (seq? possible-values)
    :onChange (fn [values]
                (on-blur (not-empty (map :value (js->clj values :keywordize-keys true)))))
    :value (map (fn [v] {:value v :label v}) value)
    :options (map (fn [v] {:value v :label v}) (remove nil? possible-values))}])

(defn text-constraint-input
  "Freeform textbox for String / Lookup constraints."
  []
  (let [focused? (reagent/atom false)]
    (fn [{:keys [value on-change on-blur disabled]}]
      [:input.form-control
       {:data-toggle "none"
        :disabled disabled
        :class (when disabled "disabled")
        :type "text"
        :value value
        :on-focus (fn [e] (reset! focused? true))
        :on-change (fn [e] (on-change (oget e :target :value)))
        :on-blur (fn [e] (on-blur (oget e :target :value)) (reset! focused? false))
        :on-key-down (fn [e] (when (= (oget e :keyCode) 13)
                               (on-blur (oget e :target :value))
                               (reset! focused? false)))}])))

(def operators-no-value
  (set (map :op (filter :no-value? operators))))

(defn constraint-input
  "Returns the appropriate input component for the constraint operator."
  [& {:keys [model path value typeahead? on-change on-blur type
             possible-values disabled op]
      :as props}]
  (cond
    (operators-no-value op)
    nil

    (= type "java.util.Date")
    [date-constraint-input props]

    (and (not= type "java.lang.Integer")
         typeahead?
         (seq? possible-values)
         (#{"=" "!="} op))
    [select-constraint-input props]

    (and typeahead?
         (seq? possible-values)
         (#{"ONE OF" "NONE OF"} op))
    [select-multiple-constraint-input props]

    :else
    [text-constraint-input props]))

(def op-type
  "Map from constraint string to either `:single` or `:multi`, corresponding
  to whether the constraint is for one or multiple values."
  (merge (zipmap (map :op (filter (complement :multiple-values?) operators))
                 (repeat :single))
         (zipmap (map :op (filter :multiple-values? operators))
                 (repeat :multi))))

(defn update-constraint
  "Takes a constraint map and a map with a new value and/or operation, and
  updates the original constraint map, making sure to update the value key if
  switching between single and multi constraints."
  [{old-op :op :as constraint} {new-op :op new-value :value :as new-const}]
  (cond-> constraint
    (contains? new-const :op)
    (as-> const
      (assoc const :op new-op)
      (case [(op-type old-op) (op-type new-op)]
        [:single :multi] (-> const
                             (set/rename-keys {:value :values})
                             (update :values #(if (empty? %) (list) (list %))))
        [:multi :single] (-> const
                             (set/rename-keys {:values :value})
                             (update :value first))
        const))
    (contains? new-const :value)
    (as-> const
      (case (op-type (:op const))
        :single (assoc const :value new-value)
        :multi (assoc const :values new-value)))))

(defn blank-constraint [loc view]
  (let [possible-values (subscribe [:selection/possible-values loc view])
        model (subscribe [:assets/model loc])]
    (fn [loc view state]
      (let [submit-constraint (fn [] (dispatch
                                      [:filters/add-constraint loc @state]
                                      (reset! state {:path view :op "=" :value nil})))
            on-constraint-change (fn [new-const]
                                   (swap! state update-constraint new-const))
            on-change (fn [v]
                        (swap! state update-constraint {:value v}))
            type (path/data-type @model view)]
        [:div.imtable-constraint
         [:div.constraint-operator
          [constraint-dropdown
           {:value (:op @state)
            :on-change on-constraint-change
            :data-type type}]]
         [:div.constraint-input
          [constraint-input
           :model @model
           :path view
           :value (or (:value @state)
                      (:values @state))
           :typeahead? true
           :on-change on-change
           :on-blur on-change
           :type type
           :possible-values @possible-values
           :disabled false
           :op (:op @state)]]]))))

(defn constraint [loc view]
  (let [possible-values (subscribe [:selection/possible-values loc view])
        model (subscribe [:assets/model loc])]
    (fn [loc view {:keys [path op value values code] :as const}]
      (let [on-constraint-change (fn [new-const]
                                   (dispatch [:filters/update-constraint loc
                                              (update-constraint const new-const)]))
            on-change (fn [v]
                        (dispatch [:filters/update-constraint loc
                                   (update-constraint const {:value v})]))
            type (path/data-type @model view)]
        [:div.imtable-constraint
         [:div.constraint-operator
          [constraint-dropdown
           {:value op
            :on-change on-constraint-change
            :data-type type}]]
         [:div.constraint-input
          [constraint-input
           :model @model
           :path path
           :value (or value values)
           :typeahead? true
           :on-change on-change
           :on-blur on-change
           :type type
           :possible-values @possible-values
           :disabled false
           :op op]]
         [:button.btn.btn-danger.constraint-delete
          {:on-click (fn [] (dispatch [:filters/remove-constraint loc const]))
           :type "button"} [:i.fa.fa-times]]]))))

(defn filter-view [loc view blank-constraint-atom]
  (let [query (subscribe [:main/temp-query loc view])]
    (fn [loc view]
      (let [active-filters (map (fn [c]
                                  [constraint loc view c])
                                (filter (partial constraint-has-path? view)
                                        (:where @query)))
            dropdown (reagent/current-component)]
        [:form.form.filter-view {:on-submit (fn [e]
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
                         (when (or (not-empty (:value @blank-constraint-atom))
                                   (not-empty (:values @blank-constraint-atom))
                                   (operators-no-value (:op @blank-constraint-atom)))
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
            friendly-name (str (string/join " " (take-last 2 (string/split (path/friendly @model view) " > "))) "s")]
        [:form.form.column-summary
         [:h4.title (str "Showing numerical distribution for " (count results) " " friendly-name)]
         [histogram/numerical-histogram results @trimmer]
         [:div.main-view
          [:div.numerical-content-wrapper
           [:table.table.table-condensed
            [:thead
             [:tr
              [:th "Min"]
              [:th "Max"]
              [:th "Average"]
              [:th "Std Deviation"]]]
            [:tbody
             [:tr
              [:td (pretty-number min)]
              [:td (pretty-number max)]
              [:td (pretty-number average)]
              [:td (pretty-number stdev)]]]]
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

(defn column-summary-title [loc view response]
  (let [model @(subscribe [:assets/model loc])
        {:keys [results uniqueValues]} response
        human-name (display-name model view)]
    [:h4.title
     (if (< (count results) uniqueValues)
       (str "Showing " (pretty-number (count results)) " of "
            (pretty-number uniqueValues) " " human-name)
       (str (pretty-number uniqueValues) " " human-name))]))

(defn column-summary-thinking []
  [css-transition-group
   {:transition-name          "fade"
    :transition-enter-timeout 50
    :transition-leave-timeout 50}
   [:div.column-summary-loader
    [:i.fa.fa-cog.fa-spin.fa-4x.fa-fw]]])

(defn column-summary [loc view local-state !dropdown]
  (let [response (subscribe [:selection/response loc view])
        selections (subscribe [:selection/selections loc view])
        text-filter (subscribe [:selection/text-filter loc view])]
    (fn [loc view local-state !dropdown]
      (cond
        (nil? @response)
        [column-summary-thinking]

        (false? @response)
        [too-many-values]

        (contains? (first (:results @response)) :min)
        [numerical-column-summary loc view (:results @response) local-state]

        :else
        [:form.form.column-summary
         [column-summary-title loc view @response]
         [:div.main-view
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
                        (when-let [el @!dropdown]
                          (-> (js/$ el)
                              (ocall! "dropdown" "toggle"))))}
           [:i.fa.fa-filter]
           (str " Filter")]]]))))

;; The 360 magic number used below is the minimum width for the column summary.
;; Instead of updating the position of the dropdown when it's done loading, I
;; went the simple route of guessing the position to be good enough. This means
;; - when dropdown is on right half of screen, the loader is not correctly
;;   aligned, but when it transitions to showing the data, it will be
;;   - it will however not be if the width of the dropdown is greater
;; - once the data is loaded, any subsequent dropdown opened will be aligned
(defn place-below!
  "Call with DOM elements to set the position styling of `below` such that it
  is directly below the edge of `above`.
  `right?` - position `below` using its top right corner (instead of top left).
  `loading?` - when `right?` is true, we need to know the width of `below` to
               position it correctly. This arg tells us that the width might
               change when it's done loading, so we just guess instead."
  [below above & {:keys [right? loading?]}]
  (let [[above-w offset-y] ((juxt #(oget % :width) #(oget % :height))
                            (gstyle/getSize above))
        offset-x (if right?
                   (* -1 (- (if loading?
                              360
                              (oget (gstyle/getSize below) :width))
                            above-w))
                   0)
        pos (-> (gstyle/getClientPosition above)
                (ocall :translate offset-x offset-y))]
    (gstyle/setPosition below pos)))

;; Bootstrap and ReactJS don't always mix well. Components that make up
;; dropdown menus are only mounted (reactjs) once and then their visibility is
;; toggled (bootstrap). These means any local state in the dropdown menu does
;; NOT get reset when the dropdown menu disappears because its never really
;; unmounts.  This was causing the new constraint textbox to retain its value
;; if a user entered something but changed their mind and closed the dropdown.
;; Reopening it would show what they previously entered. Making it look like it
;; had been applied.  To fix this we've create the blank constraint local atom
;; all the way up here at the dropdown level so that we can reset it manually,
;; and then we pass the atom down to the blank constraint component. Lame.
(defn filter-dropdown-menu [loc view idx col-count]
  (let [possible-values (subscribe [:selection/possible-values loc view])
        query (subscribe [:main/query loc view])
        blank-constraint-atom (reagent/atom {:path view :op "=" :value nil})
        !filter-dropdown (atom nil)
        !dropdown-menu (atom nil)]
    (fn [loc view idx col-count right?]
      (let [active-filters? (not-empty (filter (partial constraint-has-path? view) (:where @query)))]
        [:span.dropdown
         {:ref (comp
                (on-event
                 "hide.bs.dropdown"
                 (fn []
                    ;; Reset the blank constraint atom when the dropdown is closed
                   (reset! blank-constraint-atom {:path view :op "=" :value nil})
                    ;; *Try* to save the changes every time the dropdown is
                    ;; closed, even by just clicking off it.  This means a user
                    ;; can remove a handful of filters without having to click
                    ;; Apply. The event will do a diff to make sure something
                    ;; has actually changed before rerunning the query
                   (dispatch [:filters/save-changes loc])))
                (on-event
                  "show.bs.dropdown"
                  (fn []
                    (when (nil? @possible-values)
                      (dispatch [:main/fetch-possible-values loc view]))
                    (when-let [dropdown @!dropdown-menu]
                      (when-let [toggle @!filter-dropdown]
                        (place-below! dropdown toggle
                                      :right? right?))))))}
         [:i.fa.fa-filter.dropdown-toggle.filter-icon
          {:on-click (fn [] (dispatch [:main/set-temp-query loc]))
           :data-toggle "dropdown"
           :class (cond active-filters? "active-filter")
           :title (str "Filter " view " column")
           :ref (fn [el] (reset! !filter-dropdown el))}]
         ; Crudely try to draw the dropdown near the middle of the page
         [:div.dropdown-menu
          {:ref (fn [el] (reset! !dropdown-menu el))}
          [filter-view loc view blank-constraint-atom]]]))))

(defn summary-dropdown-menu [loc view idx col-count]
  (let [local-state (reagent/atom {})
        !summary-dropdown (atom nil)
        !dropdown-menu (atom nil)
        response (subscribe [:selection/response loc view])]
    (fn [loc view idx col-count right?]
      [:span.dropdown
       ;; Bind an event to clear the selected items when the dropdown
       ;; closes. Why don't we just avoid state all together and pick up the
       ;; checkbox values when the user clicks "Filter"? Because we still
       ;; want to know what's selected (for instance, highlighting the
       ;; histogram).
       {:ref (comp
              (on-event
               "hide.bs.dropdown"
               (fn []
                 (reset! local-state {})
                 (dispatch [:select/clear-selection loc view])))
              (on-event
               "show.bs.dropdown"
               (fn []
                 (when (nil? @response)
                   (dispatch [:main/summarize-column loc view]))
                 (when-let [dropdown @!dropdown-menu]
                   (when-let [toggle @!summary-dropdown]
                     (place-below! dropdown toggle
                                   :right? right?
                                   :loading? (nil? @response)))))))}
       [:i.fa.fa-bar-chart.dropdown-toggle
        {:title (str "Summarise " view " column")
         :data-toggle "dropdown"
         :ref (fn [el] (reset! !summary-dropdown el))}]
       [:div.dropdown-menu
        {:ref (fn [el] (reset! !dropdown-menu el))}
        [column-summary loc view local-state !summary-dropdown]]])))

(defn obj->clj [obj]
  (reduce (fn [total next-key]
            (assoc total (keyword next-key) (oget+ obj next-key))) {} (js-keys obj)))

(defn align-right? [dom-node]
  (let [{left :left} (obj->clj (ocall dom-node :getBoundingClientRect))
        screen-width (oget js/window :innerWidth)]
    (> left (/ screen-width 2))))

(defn toolbar []
  (let [right? (reagent/atom false)]
    (fn [loc view idx col-count]
      (let [sort-direction  (subscribe [:ui/column-sort-direction loc view])]
        [:div.summary-toolbar
         {:ref (fn [e]
                 (when e (reset! right? (align-right? e))))}
         [:i.fa.fa-sort.sort-icon
          {:class (case @sort-direction
                    "ASC"  "active-asc-sort"
                    "DESC" "active-desc-sort"
                    nil)
           :on-click (fn [] (dispatch [:main/sort-by loc view]))
           :title (str "Sort " view " column")}]
         [:i.fa.fa-times.remove-icon
          {:on-click (fn [] (dispatch [:main/remove-view loc view]))
           :title (str "Remove " view " column")}]
         [filter-dropdown-menu loc view idx col-count @right?]
         [summary-dropdown-menu loc view idx col-count @right?]]))))
