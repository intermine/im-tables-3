(ns im-tables.views.table.head.controls
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.graphs.histogram :as histogram]
            [oops.core :refer [oget ocall!]]))


(defn filter-input []
  (fn [view val]
    [:input.form-control
     {:type      "text"
      :value     val
      :on-change (fn [e]
                   (dispatch [:select/set-text-filter
                              view
                              (oget e :target :value)]))}]))

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


(defn column-summary [view]
  (let [response    (subscribe [:selection/response view])
        selections  (subscribe [:selection/selections view])
        text-filter (subscribe [:selection/text-filter view])]
    (reagent/create-class
      {:component-will-mount
       (fn [])
       :component-will-update
       (fn [])
       :reagent-render
       (fn [view]
         (let [close-fn (partial force-close (reagent/current-component))]
           [:form.form.min-width-250
            [histogram/main (:results @response)]
            [filter-input view @text-filter]
            [:div.max-height-400
             [:table.table.table-striped.table-condensed
              [:thead [:tr [:th] [:th "Item"] [:th "Count"]]]
              (into [:tbody]
                    (->> (filter (partial has-text? @text-filter) (:results @response))
                         (map (fn [{:keys [count item]}]
                                [:tr.hoverable
                                 {:on-click (fn [e] (dispatch [:select/toggle-selection view item]))}
                                 [:td [:div
                                       [:label
                                        [:input
                                         {:on-change (fn [])
                                          :checked   (contains? @selections item)
                                          :type      "checkbox"}]]]]
                                 [:td (if item item [:i.fa.fa-ban.mostly-transparent])]
                                 [:td
                                  [:div count]]]))))]]
            [:div.btn-toolbar
             [:button.btn.btn-primary
              {:type     "button"
               :on-click (fn []
                           (dispatch [:main/apply-summary-filter view])
                           (close-fn))}
              [:span
               [:i.fa.fa-filter]
               (str " Filter (" (count (keys @selections)) ")")]]
             (if (empty? @selections)
               [:button.btn.btn-default
                {:type     "button"
                 :on-click (fn [] (dispatch [:select/select-all view]))}
                [:span [:i.fa.fa-check-square-o] " All"]]
               [:button.btn.btn-default
                {:type     "button"
                 :disabled (empty? @selections)
                 :on-click (fn [] (dispatch [:select/clear-selection view]))}
                [:span [:i.fa.fa-square-o] " Clear"]])]]))})))

(defn toolbar []
  (fn [view]
    [:div.summary-toolbar
     [:i.fa.fa-sort
      {:on-click (fn [] (dispatch [:main/sort-by view]))}]
     [:i.fa.fa-times
      {:on-click (fn [] (dispatch [:main/remove-view view]))}]
     [:i.fa.fa-filter]
     [:span.dropdown
      [:i.fa.fa-bar-chart.dropdown-toggle {:data-toggle "dropdown"}]
      [:div.dropdown-menu
       [column-summary view]]]]))

[:div.dropdown
 [:button.dropdown-toggle {:data-toggle "dropdown"}]
 [:ul.dropdown-menu
  [:li [:a "Item 1"]]
  [:li [:a "Item 2"]]]]

(defn main []
  (fn [view]
    [:div
     [toolbar view]]))