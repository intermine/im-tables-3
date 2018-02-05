(ns im-tables.views.dashboard.manager.columns.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [imcljs.query :as query]
            [imcljs.path :as impath]
            [clojure.string :refer [join]]
            [inflections.core :refer [plural]]))

(defn tree-node []
  (let [expanded-map (reagent/atom {})]
    (fn [loc class details model current-path selected views]
      (let [attributes (get details :attributes)
            collections (get details :collections)]
        [:ul.tree-view.list-unstyled.no-select
         (into [:ul.attributes.list-unstyled]
               (map (fn [{:keys [name type]}]
                      (let [original-view? (some? (some #{(conj current-path name)} views))
                            selected? (some? (some #{(conj current-path name)} selected))]

                        [:li
                         {:on-click (fn [e]
                                      (if-not original-view?
                                        (do
                                          (dispatch [:tree-view/toggle-selection loc (conj current-path name)])))
                                      (.stopPropagation e))}
                         [:span
                          {:class (cond
                                    original-view? "label label-default"
                                    selected? "label label-success disabled")}
                          [:i.fa.fa-tag]
                          (when (and model current-path name)
                            (last (impath/display-name model (join "." (conj current-path name)))))
                          ]])) (vals attributes)))
         (into [:ul.collections.list-unstyled]
               (map (fn [{:keys [name referencedType name] :as collection}]
                      (let [referenced-class (get-in model [:classes (keyword referencedType)])]
                        [:li
                         {:on-click (fn [e] (.stopPropagation e) (swap! expanded-map update name not))}
                         [:span [:i.fa.fa-plus-square]
                          (when (and model current-path name)
                            (plural (last (impath/display-name model (join "." (conj current-path name))))))]
                         (if (get @expanded-map name)
                           [tree-node loc (keyword referencedType) referenced-class model (conj current-path name) selected views])]))
                    (sort-by (comp clojure.string/upper-case :referencedType) (vals collections))))]))))

(defn tree-view []
  (fn [loc model query selected]
    (let [sterilized-query (query/sterilize-query query)
          views (into #{} (map (fn [v] (apply conj [] (clojure.string/split v "."))) (:select sterilized-query)))]
      [:div
       [tree-node loc (keyword (:from query)) (get-in model [:classes (keyword (:from query))]) model [(:from query)] selected views]])))

(defn my-modal []
  (fn [loc]
    (let [model (subscribe [:assets/model loc])
          selected (subscribe [:tree-view/selection loc])
          query (subscribe [:main/query loc])]

      {:header []
       :body [:h1 "I am the body"]
       :footer []}

      #_[:div#myModal.modal.fade {:role "dialog"}
         [:div.modal-dialog
          [:div.modal-content
           [:div.modal-header [:h3 "Add Columns"]]
           [:div.modal-body
            [tree-view loc @model @query @selected]]
           [:div.modal-footer
            [:div.btn-toolbar.pull-right
             [:button.btn.btn-default
              {:data-dismiss "modal"}
              "Cancel"]
             [:button.btn.btn-success
              {:data-dismiss "modal"
               :disabled (< (count @selected) 1)
               :on-click (fn [] (dispatch [:tree-view/merge-new-columns loc]))}
              (str "Add " (if (> (count @selected) 0) (str (count @selected) " ")) "columns")]]]]]]
      )))

(defn modal-body []
  (fn [loc]
    (let [model (subscribe [:assets/model loc])
          selected (subscribe [:tree-view/selection loc])
          query (subscribe [:main/query loc])]

      [tree-view loc @model @query @selected]
      )))

(defn modal-footer []
  (fn [loc]
    (let [model (subscribe [:assets/model loc])
          selected (subscribe [:tree-view/selection loc])
          query (subscribe [:main/query loc])]
      [:div.btn-toolbar.pull-right
       [:button.btn.btn-default
        {:on-click (fn [] (dispatch [:prep-modal loc nil]))}
        "Cancel"]
       [:button.btn.btn-success
        {:data-dismiss "modal"
         :disabled (< (count @selected) 1)
         :on-click (fn []
                     ; Merge the new columns into the query
                     (dispatch [:tree-view/merge-new-columns loc])
                     ; Close the modal by clearing the modal value in app-db
                     (dispatch [:prep-modal loc nil]))}
        (str "Add " (if (> (count @selected) 0) (str (count @selected) " ")) "columns")]]
      )))

(defn make-modal [loc]
  {:header [:h3 "Add Columns"]
   :body [modal-body loc]
   :footer [modal-footer loc]})




(defn main []
  (fn [loc]
    [my-modal loc]))
