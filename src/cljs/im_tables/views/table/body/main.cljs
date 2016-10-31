(ns im-tables.views.table.body.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :refer [split]]
            [im-tables.components.bootstrap :refer [popover]]))

(defn dot-split
  "Convert a view such as Gene.organism.name into [:organism :name]
  for easy reaching into query result maps"
  [string]
  (into [] (drop 1 (map keyword (split string ".")))))

(defn summary-table [{:keys [value column-headers] :as summary}]
  [:table.table.table-striped.table-condensed.table-bordered
   (into [:tbody {:style {:font-size "0.9em"}}]
         (map-indexed
           (fn [idx column-header]
             (if-let [v (get-in value (dot-split (get (:views summary) idx)))]
               [:tr
                [:td (clojure.string/join " > " (drop 1 (clojure.string/split column-header " > ")))]
                [:td v]]))
           column-headers))])

(defn table-cell [{id :id}]
  (let [summary (subscribe [:summary/item-summary id])]
    (fn [{:keys [value id] :as c}]
      (let [summary-table (summary-table @summary)]
        [:td.cell {:on-click (fn [] (dispatch [:main/fetch-item-summary id]))}
         [:span {:on-mouse-enter (fn [] (dispatch [:main/summarize-item c]))
                 :data-content   summary-table
                 :data-trigger   "hover"
                 :data-placement "bottom"}
          [:div.wrapper (if value value
                                  [:i.fa.fa-ban.mostly-transparent])
           #_[:div.tooltip summary-table]]]]))))

(defn table-row [row] (into [:tr] (map (fn [c] [table-cell c])) row))