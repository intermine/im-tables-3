(ns im-tables.views.dashboard.save
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path :refer [walk class]]
            [im-tables.components.bootstrap :refer [modal]]))


(defn join-with-arrows [col]
  (clojure.string/join " > " col))

(defn save-dialog []
  (let [dom-node (reagent/atom nil)]
    (fn [state-atom details on-submit]
      [:div.container-fluid
       {:ref (fn [e] (when e (reset! dom-node e)))}
       [:div.form
        [:div.form-group
         [:label "Name"]
         [:input.form-control.input-lg
          {:value (get @state-atom :name)
           :on-change (fn [e] (swap! state-atom assoc :name (oget e :target :value)))
           :on-key-up (fn [k] (when (= 13 (oget k :keyCode))
                                (do
                                  (on-submit)
                                  (-> @dom-node js/$ (ocall :closest ".modal") (ocall :modal "hide")))))}]]]])))

(defn save-footer []
  (fn [loc state details on-submit]
    [:div.btn-toolbar.pull-right
     [:button.btn.btn-default
      {:on-click (fn [] (dispatch [:prep-modal loc nil]))}
      "Cancel"]
     [:button.btn.btn-success
      {:on-click on-submit}
      "Save"]]))

(defn generate-dialog [loc {:keys [type count query] :as details}]
  (let [state (reagent/atom {:name (str (name type) " List (" (.toString (js/Date.)) ")")})
        on-submit (fn []
                    ; Save the list
                    (dispatch [:imt.io/save-list loc (:name @state) (:query details) @state])
                    ; Close the modal by clearing the modal markup in app-db
                    (dispatch [:prep-modal loc nil])

                    )]
    {:header [:h4 (str "Save a list of " (:count details) " " (if (< count 2) (name type) (plural (name type))))]
     :body [save-dialog state details on-submit]
     :footer [save-footer loc state details on-submit]}))

(defn serialize-path [model path]
  (let [[root & remaining] (remove nil? (map :displayName (walk model path)))]
    (if remaining
      (clojure.string/join " > " (conj (map plural remaining) root))
      (plural root))))

(defn menu-heading [loc]
  (let [model (subscribe [:assets/model loc])]
    (fn [loc class details]
      [:li
       [:span
        [:h4 class]
        (into [:ul]
              (map (fn [[path query]]
                     [:li [:a
                           {
                            ;:data-toggle "modal"
                            ;:data-target "#testModal"
                            :on-click (fn [] (dispatch [:prep-modal loc
                                                        (generate-dialog loc
                                                                         {:query query
                                                                          :type class})]))}
                           (serialize-path @model path)]]) details))]])))

(defn save-menu []
  (fn [loc model path {:keys [query count]}]
    [:li
     {
      ;:data-toggle "modal"
      ;:data-target "#testModal"
      :on-click (fn [] (dispatch [:prep-modal loc
                                  (generate-dialog loc
                                                   {:query query
                                                    :count count
                                                    :type (name (path/class model path))})]))}
     [:a (str (serialize-path model path) " (" count ")")]]))


(defn main [loc]
  (let [model (subscribe [:assets/model loc])
        query-parts (subscribe [:main/query-parts loc])]
    (fn [loc]
      [:div.dropdown
       [:button.btn.btn-default.dropdown-toggle
        {:data-toggle "dropdown"} [:span [:i.fa.fa-cloud-upload] " Save List"]]
       (into [:ul.dropdown-menu]
             (->> @query-parts
                  (map-indexed (fn [idx [path details]]
                                 [save-menu loc @model path details]))))])))
