(ns im-tables.views.dashboard.manager.relationships.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :refer [atom]]
            [clojure.string :refer [join includes?]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path]
            [oops.core :refer [ocall oget]]))

(defn not-root? "String includes a dot?" [path] (includes? path "."))
(defn coll-contains? "Haystack contains needle?" [needle haystack] (some? (some #{needle} haystack)))
(defn without "Remove item from a collection" [coll item] (filter (partial not= item) coll))

(defn join-with-arrows
  "Make a friendly HTML path name: Gene >> Data Sets >> Publications"
  [v]
  (->> v
       (map (fn [n] [:span n]))
       (interpose [:span [:i.fa.fa-angle-double-right.fa-fw]])))

(defn relationship []
  (fn [loc {:keys [view model joins]}]
    (let [is-join? (coll-contains? view joins)]
      (let [add-join-fn (fn [] (dispatch [:rel-manager/toggle-relationship loc view true]))
            rmv-join-fn (fn [] (dispatch [:rel-manager/toggle-relationship loc view false]))]
        [:li.list-group-item
         [:div
          (into [:span] (join-with-arrows (path/display-name model view)))
          [:div.btn-group.pull-right
           [:button.btn {:class (if (not is-join?) "btn-primary" "btn-default")
                         :on-click rmv-join-fn}
            ; You may ask why there are so many invisible icons here - it's (unfortunately)
            ; a hack to keep things visually aligned when switching from selected to unselected.
            ; We need the checkmarks to indicate clearly which is selected!
            ; (try removing the check icons if you like;
            ;it becomes really hard to see which option is active)
            [:i {:class (if (not is-join?) "fa fa-check" "fa fa-check invisible") :aria-hidden true}]
            " Required"
            [:i {:class "fa fa-check invisible fa-fw" :aria-hidden true}]]
           [:button.btn {:class (if is-join? "btn-primary" "btn-default")
                         :on-click add-join-fn}
            [:i {:class (if is-join? "fa fa-check" "fa fa-check invisible") :aria-hidden true}]
            " Optional"
            [:i {:class "fa fa-check invisible fa-fw" :aria-hidden true}]]]]
         [:div.clearfix]]))))

(defn relationship-form [loc {:keys [query model]}]
  (fn [loc {:keys [query model]}]
    (let [relationships (distinct (filter not-root? (map (partial path/trim-to-last-class model) (:select query))))
          joins (:joins query)]
      (conj (into [:ul.list-group]
                  (when (and query model)
                    (->> relationships
                         (map (fn [view]
                                ^{:key view} [relationship loc {:view view
                                                                :joins joins
                                                                :model model}])))))))))

(defn modal-body [loc]
  (let [model (subscribe [:assets/model loc])
        rel-query (subscribe [:rel-manager/query loc])]
    (fn [loc]
      [:div
       (when @model [relationship-form loc {:query @rel-query :model @model}])])))

(defn modal-footer [loc]
  (let [model (subscribe [:assets/model loc])]
    (fn [loc]
      [:div.btn-toolbar.pull-right
       [:button.btn.btn-default {:on-click (fn [] (dispatch [:prep-modal loc nil]))} "Cancel"]
       [:button.btn.btn-success
        {:on-click (fn []
                     ; Apply the changes
                     (dispatch [:rel-manager/apply-changes loc])
                     ; Close the modal by clearing the markup from app-db
                     (dispatch [:prep-modal loc nil]))}
        "Apply Changes"]])))

(defn build-modal [loc]
  {:header [:h3 "Manage Relationships"]
   :body [modal-body loc]
   :footer [modal-footer loc]})

(defn main [loc]
  (let [rel-query (subscribe [:rel-manager/query loc])]
    (fn [loc]
      [:div
       [:div.btn-group
        [:button.btn.btn-default
         {:on-click (fn []
                      ; Reset the state of the modal
                      (dispatch [:rel-manager/reset loc])
                      ; Build the modal markup and send it to app-db
                      (dispatch [:prep-modal loc (build-modal loc)]))}
         [:i.fa.fa-share-alt] " Manage Relationships"]]])))
