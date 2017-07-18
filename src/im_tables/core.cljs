(ns im-tables.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [im-tables.events]
            [im-tables.subs]
            [im-tables.db :as db]
            [im-tables.views :as views]
            [im-tables.config :as config]
            [imcljs.query :as query]
            [re-frisk.core :refer [enable-re-frisk!]]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (enable-re-frisk!)
    (println "dev mode")))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db
                           nil
                           {
                            :service {:root "www.flymine.org/query"}
                            ;:service {:root "yeastmine.yeastgenome.org/yeastmine"}
                            :query (query/sterilize-query db/outer-join-query)}])

  (dev-setup)
  (mount-root))