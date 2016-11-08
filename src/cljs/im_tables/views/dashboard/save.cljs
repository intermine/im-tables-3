(ns im-tables.views.dashboard.save
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [imcljs.filters :as filters]))


(defn join-with-arrows [col]
  (clojure.string/join " > " col))

(defn menu-item []
  (let [model (subscribe [:assets/model])]
    (fn [path details]
      [:li [:a (str (join-with-arrows (filters/humanify @model path)) " (" (:count details) ")")]])))

;(->> columnHeaders
;     (map-indexed (fn [idx h]
;                    [table-head/header {:header h
;                                        :idx    idx
;                                        :view   (get views idx)}])))

(defn main []
  (let [query-parts (subscribe [:main/query-parts])]
    (fn [{:keys [columnHeaders views]}]
      [:div.dropdown
       [:button.btn.btn-default.dropdown-toggle
        {:data-toggle "dropdown"} "Save List"]
       (into [:ul.dropdown-menu]
             (->> @query-parts
                  (map-indexed (fn [idx [path details]] [menu-item path details]))))])))