(ns im-tables.views.table.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [im-tables.views.table.head.main :as table-head]
            [im-tables.views.table.body.main :as table-body]
            [im-tables.views.dashboard.main :as dashboard]
            [imcljs.query :as q]
            [clojure.string :refer [split]]))

(defn split-and-drop-last
  [path-str]
  (drop-last (split path-str #"\.")))

(defn split-on-dot
  [path-str]
  (split path-str #"\."))

(defn filter-not-join
  [path-str col]
  (filter #((complement clojure.string/starts-with?) % path-str) col))

(defn mapify-columns [view display-name]
  {:view view :display-name display-name})

(defn joined? [v])

(defn main [loc]
  (let [dragging-item (subscribe [:style/dragging-item loc])
        dragging-over (subscribe [:style/dragging-over loc])
        query (subscribe [:main/query])
        model (subscribe [:assets/model])]
    (fn [loc {:keys [results columnHeaders views] :as response} pagination]

      (let [joins (map split-on-dot (:joins @query))
            {:keys [limit start]} (if pagination pagination {:limit 10 :start 0})]

        (let [column-headers-without-joins (map #(filter-not-join % views) (:joins @query))]
          (map mapify-columns views columnHeaders)
          [:div.relative
          [:table.table.table-striped.table-condensed.table-bordered
           [:thead
            (into [:tr]
                  (->> columnHeaders
                       (map-indexed (fn [idx h]
                                      ^{:key (get views idx)} [table-head/header
                                                               {:header h
                                                                :dragging-over @dragging-over
                                                                :dragging-item @dragging-item
                                                                :loc loc
                                                                :idx idx
                                                                :col-count (count columnHeaders)
                                                                :view (get views idx)}]))))]
           (into [:tbody]
                 (->>
                   (map second (into (sorted-map) (select-keys results (range start (+ start limit)))))
                   ;(take (:limit pagination) (drop (:start pagination) results))
                   (map (fn [r] [table-body/table-row loc r]))))]])))))
