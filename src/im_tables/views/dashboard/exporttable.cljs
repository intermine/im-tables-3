(ns im-tables.views.dashboard.exporttable
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path :refer [walk class]]
            [im-tables.components.bootstrap :refer [modal]]))

;;todo: make this live in the db, possibly a per-mine config
(def valid-formats [:tsv :csv])

(defn modal-body [loc]
  (fn []
    (reduce
       (fn [select format]
         (conj select [:option (name format)])) [:select {:on-change #(dispatch [:exporttable/set-format loc (oget % "target" "value")])}] valid-formats)))

(defn export-menu [loc]
    {:header [:h4 "Export this table as..."]
     :body [modal-body loc]
     :footer [:button {:on-click (fn [] (dispatch [:exporttable/download loc]))} "download now!"]
     })

(defn exporttable [loc]
  [:button.btn.btn-default
   {:data-toggle "modal"
    :data-target "#testModal"
    :type "button"
    :on-click
    (fn [e]

      (dispatch [:prep-modal loc (export-menu loc)]))} "Export"])
