(ns im-tables.views.table.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.head.main :as table-head]
            [im-tables.views.table.body.main :as table-body]
            [im-tables.views.dashboard.main :as dashboard]))

(defn main []
  (let [pagination (subscribe [:settings/pagination])]
    (fn [{:keys [results columnHeaders views] :as response}]
      [:div.relative
       [:table.table.table-striped.table-condensed.table-bordered
        [:thead
         (into [:tr]
               (->> columnHeaders
                    (map-indexed (fn [idx h]
                                   [table-head/header {:header h
                                                       :idx    idx
                                                       :view   (get views idx)}]))))]
        (into [:tbody]
              (->> (take (:limit @pagination) (drop (:start @pagination) results))
                   (map (fn [r] [table-body/table-row r]))))]
       ;[:div.overlay]
       ])))