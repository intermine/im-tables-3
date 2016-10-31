(ns im-tables.views.core
  (:require [re-frame.core :refer [subscribe dispatch]]
            [im-tables.views.dashboard.main :as dashboard]
            [im-tables.views.table.core :as table]
            ))

(defn main []
  (let [response   (subscribe [:main/query-response])
        pagination (subscribe [:settings/pagination])]
    (fn []
      [:div
       [dashboard/main @response @pagination]
       [table/main @response]])))