(ns im-tables.views.dashboard.source
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.utils :refer [on-event]]
            [imcljs.path :as im-path]))

(defn main [loc]
  (let [model @(subscribe [:assets/model loc])
        parts (->> @(subscribe [:main/query-parts loc])
                   (keys)
                   (keep (fn [path]
                           (when-some [datasets @(subscribe [:source/part loc path])]
                             [path datasets]))))]
    [:div.dropdown
     {:ref (on-event
            "show.bs.dropdown"
            (fn []
              (dispatch [:source/fetch-parts loc])))}
     [:button.btn.btn-default.dropdown-toggle
      {:data-toggle "dropdown"}
      [:span [:i.fa.fa-database] " Source"]]
     (into [:ul.dropdown-menu.source-dropdown]
           (for [[path datasets] parts]
             (into [:<>
                    [:li [:a.disabled.section-heading
                          [:strong (->> path (im-path/walk model) peek
                                        ((some-fn :displayName :name)))]]]]
                   (if (coll? datasets) ; datasets can be a non-empty collection or false.
                     (for [{:keys [description url name]} datasets]
                       [:li [:a {:href url :target "_blank" :title description} name]])
                     [[:li [:a.disabled "Failed to fetch datasets"]]]))))]))
