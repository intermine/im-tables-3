(ns im-tables.views.table.head.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.table.head.summaries :as summaries]
            [clojure.string :refer [join split]]))

(defn header []
  (fn [{:keys [header view]}]
    (let [[class & path] (split header " > ")] [:th
      [summaries/main view]
      [:div
       [:div class]
       [:div (join " . " path)]]])))