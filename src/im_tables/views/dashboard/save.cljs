(ns im-tables.views.dashboard.save
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path :refer [walk class]]
            [im-tables.components.bootstrap :refer [modal]]
            [im-tables.utils :refer [on-event]]))

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
  (fn [loc state {:keys [picking?] :as details} on-submit]
    [:div.btn-toolbar.pull-right
     [:button.btn.btn-default
      {:on-click (fn [_evt]
                   (dispatch [:modal/close loc])
                   (when picking?
                     (dispatch [:pick-items/stop loc])))}
      "Cancel"]
     [:button.btn.btn-raised.btn-success
      {:on-click on-submit}
      "Save"]]))

(defn generate-dialog [loc {:keys [type count query picking?] :as details}]
  (let [state (reagent/atom {:name (str (name type) " List (" (.toString (js/Date.)) ")")})
        on-submit (fn []
                    (dispatch [:imt.io/save-list loc (:name @state) (:query details) @state])
                    (dispatch [:modal/close loc])
                    (when picking?
                      (dispatch [:pick-items/stop loc])))]

    {:header [:h4 (str "Save a list of " (:count details) " "
                       ;; It's possible that `count` is the string "..." if
                       ;; the webservice hasn't responded yet.
                       (if (and (number? count) (< count 2))
                         (name type)
                         (plural (name type))))]
     :body [save-dialog state details on-submit]
     :footer [save-footer loc state details on-submit]
     :no-fade picking?}))

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
                     [:li [:a {:on-click
                               #(dispatch [:modal/open loc
                                           (generate-dialog loc
                                                            {:query query
                                                             :type class})])}
                           (serialize-path @model path)]]) details))]])))

(defn save-menu [loc _model _path _details]
  (let [counts (subscribe [:main/query-parts-counts loc])]
    (fn [loc model path {:keys [query]}]
      (let [count (get @counts path "...")]
        [:li {:on-click
              #(dispatch [:modal/open loc
                          (generate-dialog loc
                                           {:query query
                                            :count count
                                            :type (name (path/class model path))})])}
         [:a [:strong (serialize-path model path)] (str " (" count ")")]]))))

(defn main [loc]
  (let [model       (subscribe [:assets/model loc])
        query-parts (subscribe [:main/query-parts loc])
        counts      (subscribe [:main/query-parts-counts loc])]
    (fn [loc]
      [:div.dropdown
       {:ref (on-event
              "show.bs.dropdown"
              #(when (nil? @counts)
                  ;; This will only run for the initial query. For any
                  ;; subsequent queries, we'll `:main/count-deconstruction`
                  ;; alongside the query.
                 (doseq [event (map (fn [[part details]]
                                      [:main/count-deconstruction loc part details])
                                    @query-parts)]
                   (dispatch event))))}
       [:button.btn.btn-default.dropdown-toggle
        {:data-toggle "dropdown"} [:span [:i.fa.fa-cloud-upload] " Save List"]]
       (-> [:ul.dropdown-menu]
           (into (map-indexed (fn [idx [path details]]
                                [save-menu loc @model path details])
                              @query-parts))
           (conj [:hr]
                 [:li {:on-click #(dispatch [:pick-items/start loc])}
                  [:a "Pick items from the table"]]))])))
