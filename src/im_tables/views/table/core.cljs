(ns im-tables.views.table.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.table.head.main :as table-head]
            [im-tables.views.table.body.main :as table-body]
            [im-tables.views.dashboard.main :as dashboard]))


(defn main [loc]
  (let [dragging-item (subscribe [:style/dragging-item loc])
        dragging-over (subscribe [:style/dragging-over loc])]
    (fn [loc {:keys [results columnHeaders views] :as response} pagination]
      (let [pagination (if pagination pagination {:limit 10 :start 0})]
        [:div.relative
         [:table.table.table-striped.table-condensed.table-bordered
          [:thead
           (into [:tr]
                 (->> columnHeaders
                      (map-indexed (fn [idx h]
                                     ^{:key (get views idx)} [table-head/header
                                                              {:header        h
                                                               :dragging-over @dragging-over
                                                               :dragging-item @dragging-item
                                                               :loc           loc
                                                               :idx           idx
                                                               :view          (get views idx)}]))))]
          (into [:tbody]
                (->> (take (:limit pagination) (drop (:start pagination) results))
                     (map (fn [r] [table-body/table-row loc r]))))]]))))