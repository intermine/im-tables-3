(ns im-tables.views.dashboard.exporttable
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path :refer [walk class]]
            [im-tables.components.bootstrap :refer [modal]]))

(defn format-dropdown
  "creates the dropdown to allow users to select their preferred format"
  [loc]
  (let [settings @(subscribe [:settings/settings loc])
        query-parts @(subscribe [:main/query-parts loc])
        {:keys [accepted-formats order-formats]} (get-in settings [:data-out])
        valid-export-formats (->> (map (juxt identity accepted-formats) order-formats)
                                  (keep (fn [[format suitable-for]]
                                          (when (or (= suitable-for :all)
                                                    ;; If not :all, it's a vector of classes which the query needs to have.
                                                    (some #(contains? query-parts (name %)) suitable-for))
                                            format))))]
    (into [:select.form-control
           {:on-change #(dispatch [:exporttable/set-format loc (oget % "target" "value")])}]
          (for [format valid-export-formats]
            [:option (name format)]))))

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

