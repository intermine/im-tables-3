(ns im-tables.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [re-frame.core :as re-frame]
            [im-tables.events]
            [im-tables.subs]
            [im-tables.db :as db]
            [im-tables.views :as views]
            [im-tables.views.core :refer [main]]
            [im-tables.config :as config]
            [imcljs.query :as query]
            [cljsjs.react-transition-group]
            [cljsjs.react-day-picker]
            [cljsjs.react-select]
            [cljsjs.highlight]
            [cljsjs.highlight.langs.javascript]
            [cljsjs.highlight.langs.perl]
            [cljsjs.highlight.langs.python]
            [cljsjs.highlight.langs.ruby]
            [cljsjs.highlight.langs.java]
            [cljsjs.highlight.langs.xml]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (dom/render [views/main-panel]
              (.getElementById js/document "app")))

(defn table
  "This is the primary means of including an im-table in a re-frame project.
  Argument can be either a map with at minimum `:location`, `:service` and
  `:query` keys, or a location vector. In the latter case you will need to
  dispatch `:im-tables/load` yourself, with the location and map as arguments."
  [{:keys [location] :as args}]
  (when (map? args)
    (re-frame/dispatch [:im-tables/load location (dissoc args :location)]))
  (fn [{:keys [location] :as loc}]
    [main (or location loc)]))

(defn ^:export init []
  (views/reboot-tables-fn)
  (dev-setup)
  (mount-root))
