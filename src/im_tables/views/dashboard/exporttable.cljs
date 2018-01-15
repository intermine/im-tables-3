(ns im-tables.views.dashboard.exporttable
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path :refer [walk class]]
            [im-tables.components.bootstrap :refer [modal]]))


(defn check-if-good
  "checks if certain formats are suitable for export given the data in the table
    Right now this is explicitly for FASTA, which requires a gene or protein.
    Returns either the list of acceptable formats as it was, or the same list
    with the new possible format appended to the end. "
  [good-formats model-parts suitable-for format]
  (let [model-bits (set (keys model-parts))]
  (distinct
   (reduce
    (fn [suitable-bit]
     (if (contains? model-bits (name suitable-bit))
       (conj good-formats format)
       good-formats)
      ) suitable-for))))

(defn modal-body
  "creates the dropdown to allow users to select their preferred format"
  [loc]
  (fn []
    (let [settings (subscribe [:settings/settings loc])
          model-parts (subscribe [:main/query-parts loc])
          export-formats (get-in @settings [:data-out :accepted-formats])
          valid-export-formats
            (reduce (fn [good-formats [format suitable-for]]
                                         (if (= suitable-for :all)
                                           (conj good-formats format)
                                           (check-if-good good-formats @model-parts suitable-for format)
                                           )) [] export-formats)]
    (reduce
       (fn [select format]
         (conj select [:option (name format)]))
       [:select.form-control
        {:on-change
         #(dispatch [:exporttable/set-format loc (oget % "target" "value")])}] valid-export-formats))))

(defn export-menu
  "UI element. Presents the modal to allow user to select an export format."
  [loc]
    {:header [:h4 "Export this table as..." [:a.close {:on-click #(dispatch [:modal/close loc])} "x"]]
     :body [:div.modal-body
            [:form [:label "Select a format" [modal-body loc]]]
            [:a {:id "hiddendownloadlink" :download "download"}]]
     :footer [:button.btn.btn-primary {:on-click (fn [] (dispatch [:exporttable/download loc]))} "Download now!"]
     })

(defn exporttable [loc]
  [:button.btn.btn-default
   {
    ;:data-toggle "modal"
    ;:data-target "#testModal"
    :type "button"
    :on-click
    (fn [e]
      (dispatch [:prep-modal loc (export-menu loc)])
      ;(dispatch [:prep-modal loc [:div [:h1 {:on-click (fn [] (println "TEST"))} "Test"] [:p "thanks"]]])

      )}
   [:i.fa.fa-download] " Export"])

