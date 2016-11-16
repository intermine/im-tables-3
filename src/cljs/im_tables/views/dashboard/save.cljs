(ns im-tables.views.dashboard.save
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [im-tables.components.bootstrap :refer [modal]]))


(defn join-with-arrows [col]
  (clojure.string/join " > " col))

(defn save-dialog []
  (fn [state-atom]
    [:div.container-fluid
     [:form.form
      [:div.form-group
       [:label "Name"]
       [:input.form-control.input-lg
        {:value     (get @state-atom :name)
         :on-change (fn [e] (swap! state-atom assoc :name (oget e :target :value)))}]]]]))

(defn save-footer []
  (fn [state details]
    [:div.btn-toolbar.pull-right
     [:button.btn.btn-default
      {:data-dismiss "modal"}
      "Cancel"]
     [:button.btn.btn-success
      {:data-dismiss "modal"
       :on-click     (fn [] (dispatch [:save (:query details) @state]))}
      "Save"]]))

(defn generate-dialog [{:keys [type count query] :as details}]
  (let [state (reagent/atom {:name (str (name type) " List (" (.toString (js/Date.)) ")")})]
    {:header [:h4 (str "Save a list of " (:count details) " " (if (< count 2) (name type) (plural (name type))))]
     :body   [save-dialog state details]
     :footer [save-footer state details]}))

(defn menu-item []
  (let [model (subscribe [:assets/model])]
    (fn [path details]
      [:li
       {:data-toggle "modal"
        :data-target "#testModal"
        :on-click    (fn [] (dispatch [:prep-modal (generate-dialog details)]))}
       [:a (str
             ;(join-with-arrows (filters/humanify @model path))
             path
             " (" (:count details) ")")]])))

(defn main []
  (let [query-parts (subscribe [:main/query-parts])]
    (fn []
      [:div
       [:button.btn.btn-primary.btn-raised
        {:on-click (fn [] (dispatch [:xmlify]))} "XML"]
       [:div.dropdown
        [:button.btn.btn-default.dropdown-toggle
         {:data-toggle "dropdown"} "Save List"]
        (into [:ul.dropdown-menu]
              (->> @query-parts
                   (map-indexed (fn [idx [path details]] [menu-item path details]))))]])))

