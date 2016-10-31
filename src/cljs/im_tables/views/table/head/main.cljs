(ns im-tables.views.table.head.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.head.controls :as controls]
            [clojure.string :refer [join split]]))

(defn header []
  (fn [{:keys [header view]}]
    (let [[class & path] (split header " > ")] [:th
      [controls/main view]
      [:div
       [:div class]
       [:div (join " . " path)]]])))