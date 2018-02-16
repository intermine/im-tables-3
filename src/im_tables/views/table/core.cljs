(ns im-tables.views.table.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.table.head.main :as table-head]
            [im-tables.views.table.body.main :as table-body]
            [im-tables.views.dashboard.main :as dashboard]
            [imcljs.query :as q]
            [imcljs.path :as im-path]
            [clojure.string :refer [split starts-with?]]))

(defn table-head [loc]
  (let [dragging-item   (subscribe [:style/dragging-item loc])
        dragging-over   (subscribe [:style/dragging-over loc])
        collapsed-views (subscribe [:query-response/views-collapsed-by-joins loc])
        model           (subscribe [:assets/model loc])]
    (fn [views]
      [:thead
       (into [:tr]
             (->> @collapsed-views
                  (map-indexed (fn [idx h]
                                 ^{:key (get views idx)}
                                 [table-head/header loc
                                  {:header        (if-not (seq? h) h (when (and @model (first h))
                                                                       (js/console.log "TRYING"  @model (first h))
                                                                       (im-path/trim-to-last-class @model (first h))))
                                   :dragging-over @dragging-over
                                   :dragging-item @dragging-item
                                   :loc           loc
                                   :idx           idx
                                   :subviews      (when (seq? h) h)
                                   :col-count     (count @collapsed-views)
                                   :view          (if-not (seq? h) h (when (and @model (first h)) (im-path/trim-to-last-class @model (first h))))}]))))])))

(defn main [loc]
  (let [dragging-item   (subscribe [:style/dragging-item loc])
        dragging-over   (subscribe [:style/dragging-over loc])
        collapsed-views (subscribe [:query-response/views-collapsed-by-joins loc])]
    (fn [loc {:keys [results views]} {:keys [limit start] :or {limit 10 start 0}}]
      [:div.relative
       [:table.table.table-condensed.table-bordered.table-striped
        [table-head loc views]
        (into [:tbody]
              (->>
                (map second (into (sorted-map) (select-keys results (range start (+ start limit)))))
                (map (fn [r] [table-body/table-row loc r]))))]])))
