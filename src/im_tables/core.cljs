(ns im-tables.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as re-frame]
            [im-tables.events]
            [im-tables.subs]
            [im-tables.db :as db]
            [im-tables.views :as views]
            [im-tables.config :as config]
            [imcljs.query :as query]
            [cljsjs.react-transition-group]
            [cljsjs.highlight]
            [cljsjs.highlight.langs.javascript]
            [cljsjs.highlight.langs.perl]
            [cljsjs.highlight.langs.python]
            [cljsjs.highlight.langs.ruby]
            [cljsjs.highlight.langs.java]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (dom/render [views/main-panel]
              (.getElementById js/document "app")))

(defn ^:export init []
  #_(re-frame/dispatch-sync [:initialize-db
                             [:test :location]
                             {:service {:root "www.flymine.org/query"}
                              ;:service {:root "yeastmine.yeastgenome.org/yeastmine"}
                              :query (query/sterilize-query db/outer-join-query)}])

  (dev-setup)
  (mount-root))
