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
                           {:service {:root "yeastmine.yeastgenome.org/yeastmine"}
                            :query (query/sterilize-query db/outer-join-query)}])

  (dev-setup)
  (mount-root))

{:from   "Gene"
 :select ["secondaryIdentifier"
          "symbol"
          "primaryIdentifier"
          "organism.name"
          "homologues.homologue.symbol"
          "homologues.homologue.organism.name"
          ]
 :where [{:path "Gene"
          :op "IN"
          :value "PL FlyAtlas_maleglands_top"}]}