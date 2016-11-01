(ns im-tables.views
    (:require [re-frame.core :as re-frame :refer [dispatch]]
              [im-tables.views.core :as main-view]))

(defn main-panel []
  (fn []
    [:div
     [:div.panel.panel-default
      [:div.panel-body

       ;[:h1 "IM-Tables CLJS"]
       ]]
     [:div.container
      [main-view/main]]]))
