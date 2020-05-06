(ns im-tables.views.table.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.table.head.main :as table-head]
            [im-tables.views.table.body.main :as table-body]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.error :as error]
            [imcljs.query :as q]
            [im-tables.utils :refer [response->error]]))

(defn table-head [loc]
  (let [dragging-item (subscribe [:style/dragging-item loc])
        dragging-over (subscribe [:style/dragging-over loc])
        collapsed-views (subscribe [:query-response/views-collapsed-by-joins loc])]
    (fn [loc views]
      [:thead
       (into [:tr]
             (->> @collapsed-views
                  (map-indexed (fn [idx h]
                                 ^{:key (get views idx)}
                                 [table-head/header loc
                                  {:header h
                                   :dragging-over @dragging-over
                                   :dragging-item @dragging-item
                                   :loc loc
                                   :idx idx
                                   :subviews nil
                                   :col-count (count @collapsed-views)
                                   :view h}]))))])))

(defn handle-states
  "Depending on the response, other states may be displayed instead of children.
  Note that `res` might not hold the latest response if it failed, as in this
  case the `on-failure` event would fire instead."
  [loc {:keys [results success wasSuccessful] :as res} & children]
  (let [error @(subscribe [:main/error loc])
        successful? (or success wasSuccessful)]
    (cond
      error         [error/failure loc error]
      (seq results) (into [:<>] children)
      successful?   [error/no-results loc]
      (nil? res)    nil
      ;; The else case shouldn't occur, but we leave it just in case!
      :else         [error/failure loc (response->error res)])))

(defn main [loc
            {:keys [results views] :as res}
            {:keys [limit start] :or {limit 10 start 0}}]
  [handle-states loc res
   [:div.table-container
    [:table.table.table-condensed.table-bordered.table-striped
     [table-head loc views]
     (into [:tbody]
           (->> (range start (+ start limit))
                (select-keys results)
                (into (sorted-map))
                (map second)
                (map (fn [r] [table-body/table-row loc r]))))]]])
