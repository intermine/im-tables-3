(ns im-tables.views
    (:require [re-frame.core :as re-frame :refer [dispatch]]
              [im-tables.views.core :as main-view]))

(defn main-panel []
  (fn []
    [:div.container
     [main-view/main {:path [:nest]}]]))
