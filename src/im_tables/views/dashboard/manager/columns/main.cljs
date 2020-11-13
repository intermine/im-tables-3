(ns im-tables.views.dashboard.manager.columns.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [imcljs.query :as query]
            [imcljs.path :as impath]
            [clojure.string :refer [join split]]
            [inflections.core :refer [plural]]))

(defn tree-node []
  (let [expanded-map (reagent/atom {})]
    (fn [loc class details model current-path selected views path->subclass]
      (let [attributes     (vals (get details :attributes))
            collections    (vals (get details :collections))
            references     (vals (get details :references))
            colls-and-refs (concat collections references)]
        [:ul.tree-view.list-unstyled.no-select
         (into [:ul.attributes.list-unstyled]
               (map (fn [{:keys [name type]}]
                      (let [original-view? (some? (some #{(conj current-path name)} views))
                            selected? (some? (some #{(conj current-path name)} selected))]

                        [:li
                         {:on-click (fn [e]
                                      (when-not original-view?
                                        (dispatch [:tree-view/toggle-selection loc (conj current-path name)]))
                                      (.stopPropagation e))}
                         [:span
                          {:class (cond
                                    original-view? "label label-default"
                                    selected? "label label-success disabled")}
                          [:i.fa.fa-tag]
                          (when (and model current-path name)
                            (last (impath/display-name model (join "." (conj current-path name)))))]])) attributes))
         (into [:ul.collections.list-unstyled]
               (map (fn [{:keys [referencedType name]}]
                      (let [next-class (get path->subclass (conj current-path name) (keyword referencedType))
                            referenced-class (get-in model [:classes next-class])]
                        [:li
                         {:on-click (fn [e] (.stopPropagation e) (swap! expanded-map update name not))}
                         [:span [:i.fa.fa-plus-square]
                          (when (and model current-path name)
                            (plural (last (impath/display-name model (join "." (conj current-path name))))))]
                         (when (get @expanded-map name)
                           [tree-node loc next-class referenced-class model (conj current-path name) selected views path->subclass])]))
                    (sort-by (comp clojure.string/upper-case :displayName) colls-and-refs)))]))))

(defn tree-view []
  (fn [loc model query selected]
    (let [{root-class :from} query
          sterilized-query (query/sterilize-query query)
          views (into #{}
                      (map (fn [path]
                             (split path #"\."))
                           (:select sterilized-query)))
          path->subclass (->> (:type-constraints model)
                              (filter #(contains? % :type))
                              (reduce (fn [m {:keys [path type]}]
                                        (assoc m (split path #"\.") (keyword type)))
                                      {}))]
      [:div
       [tree-node loc (keyword root-class) (get-in model [:classes (keyword root-class)]) model [root-class] selected views path->subclass]])))

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
             [:button.btn.btn-raised.btn-success
              {:data-dismiss "modal"
               :disabled (< (count @selected) 1)
               :on-click (fn [] (dispatch [:tree-view/merge-new-columns loc]))}
              (str "Add " (if (> (count @selected) 0) (str (count @selected) " ")) "columns")]]]]]])))

(defn modal-body []
  (fn [loc]
    (let [model (subscribe [:assets/model loc])
          selected (subscribe [:tree-view/selection loc])
          query (subscribe [:main/query loc])]

      [tree-view loc @model @query @selected])))

(defn modal-footer []
  (fn [loc]
    (let [model (subscribe [:assets/model loc])
          selected (subscribe [:tree-view/selection loc])
          query (subscribe [:main/query loc])]
      [:div.btn-toolbar.pull-right
       [:button.btn.btn-default
        {:on-click #(dispatch [:modal/close loc])}
        "Cancel"]
       [:button.btn.btn-raised.btn-success
        {:data-dismiss "modal"
         :disabled (< (count @selected) 1)
         :on-click (fn []
                     ; Merge the new columns into the query
                     (dispatch [:tree-view/merge-new-columns loc])
                     ; Close the modal by clearing the modal value in app-db
                     (dispatch [:modal/close loc]))}
        (str "Add " (if (> (count @selected) 0) (str (count @selected) " ")) "columns")]])))

(defn make-modal [loc]
  {:header [:h3 "Add Columns"]
   :body [modal-body loc]
   :footer [modal-footer loc]})

(defn main []
  (fn [loc]
    [my-modal loc]))
