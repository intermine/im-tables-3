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

(defn ^:export init []
  (views/reboot-tables-fn)
  (dev-setup)
  (mount-root))
