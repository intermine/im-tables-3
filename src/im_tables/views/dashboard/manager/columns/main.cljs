(ns im-tables.views.dashboard.manager.columns.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [imcljs.query :as query]
            [imcljs.path :as impath]
            [clojure.string :refer [join split]]
            [inflections.core :refer [plural]]))

(defn tree-node []
  (fn [loc expanded-paths* details model current-path selected views path->subclass]
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
                          (last (impath/display-name model (join "." (conj current-path name)))))]]))
                  attributes))
       (into [:ul.collections.list-unstyled]
             (map (fn [{:keys [referencedType name]}]
                    (let [this-path (conj current-path name)
                          is-expanded (get @expanded-paths* this-path)
                          next-class (get path->subclass this-path (keyword referencedType))
                          referenced-class (get-in model [:classes next-class])]
                      [:li
                       {:on-click (fn [e] (.stopPropagation e) (swap! expanded-paths* (if is-expanded disj conj) this-path))}
                       [:span [:i.fa.fa-plus-square]
                        (when (and model current-path name)
                          (plural (last (impath/display-name model (join "." (conj current-path name))))))]
                       (when is-expanded
                         [tree-node loc expanded-paths* referenced-class model (conj current-path name) selected views path->subclass])]))
                  (sort-by (comp clojure.string/upper-case :displayName) colls-and-refs)))])))

(defn pre-expanded-paths
  "Returns a set of paths corresponding to all parent paths of `views`, in effect, expanding them."
  [views]
  (into #{}
       (mapcat (fn [path]
                 (->> (split path #"\.")
                      (iterate drop-last)
                      (take-while seq)
                      (next))))
       views))

(defn tree-view [loc model query]
  (let [expanded-paths* (reagent/atom (->> query query/sterilize-query :select pre-expanded-paths))]
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
         [tree-node loc expanded-paths* (get-in model [:classes (keyword root-class)]) model [root-class] selected views path->subclass]]))))

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
