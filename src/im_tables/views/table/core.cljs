(ns im-tables.views.table.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.table.head.main :as table-head]
            [im-tables.views.table.body.main :as table-body]
            [im-tables.views.dashboard.main :as dashboard]
            [imcljs.query :as q]
            [clojure.string :refer [split starts-with?]]))

(defn main [loc]
  (let [dragging-item (subscribe [:style/dragging-item loc])
        dragging-over (subscribe [:style/dragging-over loc])
        collapsed-views (subscribe [:query-response/views-collapsed-by-joins])]
    (fn [loc {:keys [results views]} {:keys [limit start] :or {limit 10 start 0}}]
      [:div.relative
       [:table.table.table-condensed.table-bordered.table-striped
        [:thead
         (into [:tr]
               (->> @collapsed-views
                    (map-indexed (fn [idx h]
                                   ^{:key (get views idx)}
                                   [table-head/header
                                    {:header h
                                     :dragging-over @dragging-over
                                     :dragging-item @dragging-item
                                     :loc loc
                                     :idx idx
                                     :subviews nil
                                     :col-count (count @collapsed-views)
                                     :view h}]))))]
        (into [:tbody]
              (->>
                (map second (into (sorted-map) (select-keys results (range start (+ start limit)))))
                (map (fn [r] [table-body/table-row loc r]))))]])))
