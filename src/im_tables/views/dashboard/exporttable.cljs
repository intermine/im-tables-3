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
    (distinct (reduce (fn [suitable-bit]
                        (if (contains? model-bits (name suitable-bit))
                          (conj good-formats format)
                          good-formats))
                      suitable-for))))

(defn format-dropdown
  "creates the dropdown to allow users to select their preferred format"
  [loc]
  (let [settings @(subscribe [:settings/settings loc])
        model-parts @(subscribe [:main/query-parts loc])
        export-formats (get-in settings [:data-out :accepted-formats])
        valid-export-formats (reduce (fn [good-formats [format suitable-for]]
                                       (if (= suitable-for :all)
                                         (conj good-formats format)
                                         (check-if-good good-formats model-parts suitable-for format)))
                                     [] export-formats)]
    (reduce (fn [select format]
              (conj select [:option (name format)]))
            [:select.form-control
             {:on-change #(dispatch [:exporttable/set-format loc (oget % "target" "value")])}]
            valid-export-formats)))

(defn toggle-or-switch [target]
  (fn [prev-value]
    (if (= prev-value target)
      nil
      target)))

(defn modal-body
  [loc]
  (let [data-out (subscribe [:settings/data-out loc])
        expanded* (reagent/atom (cond
                                  (:export-data-package @data-out) :data-package
                                  (:compression @data-out) :compression))]
    (fn [loc]
      (let [{:keys [export-data-package compression]} @data-out]
        [:div.modal-body.exporttable-body
         [:form [:label "Select a format" [format-dropdown loc]]]
         [:div.export-options
          [:div.panel.panel-default
           [:div.panel-heading
            {:class (when (= @expanded* :data-package) :active)
             :on-click #(swap! expanded* (toggle-or-switch :data-package))}
            [:h3.panel-title "Frictionless Data Package"]]
           (when (= @expanded* :data-package)
             [:div.panel-body
              [:label
               [:input {:type "checkbox"
                        :on-change #(dispatch [:exporttable/toggle-export-data-package loc])
                        :checked export-data-package}]
               " Export Frictionless Data Package (uses ZIP compression)"]])]
          [:div.panel.panel-default
           [:div.panel-heading
            {:class (when (= @expanded* :compression) :active)
             :on-click #(swap! expanded* (toggle-or-switch :compression))}
            [:h3.panel-title "Compression"]]
           (when (= @expanded* :compression)
             [:div.panel-body
              (when export-data-package
                [:p [:strong "Frictionless Data Package uses ZIP Compression only."]])
              [:label
               [:input {:type "radio"
                        :name "compression-type"
                        :disabled export-data-package
                        :on-change #(dispatch [:exporttable/set-compression loc nil])
                        :checked (nil? compression)}]
               " No compression"]
              [:label
               [:input {:type "radio"
                        :name "compression-type"
                        :disabled export-data-package
                        :on-change #(dispatch [:exporttable/set-compression loc :zip])
                        :checked (= compression :zip)}]
               " Use Zip compression (produces a .zip archive)"]
              [:label
               [:input {:type "radio"
                        :name "compression-type"
                        :disabled export-data-package
                        :on-change #(dispatch [:exporttable/set-compression loc :gzip])
                        :checked (= compression :gzip)}]
               " Use GZIP compression (produces a .gzip archive)"]])]]]))))

(defn modal-footer
  "Clicking the anchor element will cause the file to be downloaded directly
  from the server due to it responding with content-disposition: attachment."
  [loc]
  (let [href @(subscribe [:export/download-href loc])]
    [:a.btn.btn-raised.btn-primary
     {:href href}
     "Download now!"]))

(defn export-menu
  "UI element. Presents the modal to allow user to select an export format."
  [loc]
  {:header [:h4 "Export this table as..." [:a.close {:on-click #(dispatch [:modal/close loc])} "x"]]
   :body [modal-body loc]
   :footer [modal-footer loc]})

(defn exporttable [loc]
  [:button.btn.btn-default
   {:type "button"
    :on-click #(dispatch [:modal/open loc (export-menu loc)])}
   [:i.fa.fa-download] " Export"])

