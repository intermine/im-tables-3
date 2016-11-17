(ns im-tables.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [im-tables.events]
              [im-tables.subs]
              [im-tables.views :as views]
              [im-tables.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db [:store :here]])
  (dev-setup)
  (mount-root))
