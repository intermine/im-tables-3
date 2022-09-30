(ns im-tables.views.dashboard.exporttable
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [oops.core :refer [oget ocall ocall!]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path :refer [walk class]]
            [im-tables.components.bootstrap :refer [modal]]))

(def format->styling
  {:tsv [:span [:i.fa.fa-file-excel-o] " Tab separated values"]
   :csv [:span [:i.fa.fa-file-excel-o] " Comma separated values"]
   :xml [:span [:i.fa.fa-code] " XML"]
   :json [:span "{ } JSON"]
   :fasta [:span [:i.fa.fa-exchange] " FASTA sequence"]
   :gff3 [:span [:i.fa.fa-exchange] " GFF3 features"]
   :bed [:span [:i.fa.fa-exchange] " BED locations"]
   :rdf [:span [:i.fa.fa-sitemap] " RDF"]
   :ntriples [:span [:i.fa.fa-sitemap] " N-Triples"]})

(defn bioinf? [format] (contains? #{"fasta" "gff3" "bed"} format))
(defn tabular? [format] (contains? #{"tsv" "csv"} format))

(defn format-dropdown
  "creates the dropdown to allow users to select their preferred format"
  [loc {:keys [format accepted-formats order-formats]}]
  (let [query-parts @(subscribe [:main/query-parts loc])
        valid-export-formats (->> (map (juxt identity accepted-formats) order-formats)
                                  (keep (fn [[format suitable-for]]
                                          (when (or (= suitable-for :all)
                                                    ;; If not :all, it's a vector of classes which the query needs to have.
                                                    (some #(contains? query-parts (name %)) suitable-for))
                                            format))))]
    [:div.dropdown
     [:button.btn.btn-default.btn-toolbar.dropdown-toggle
      {:data-toggle "dropdown"}
      format]
     (into [:ul.dropdown-menu.dropdown-menu-right]
           (for [format valid-export-formats]
             [:li
              [:button.btn.btn-link.btn-block
               {:on-click #(dispatch [:exporttable/set-format loc (name format)])}
               (get format->styling format (name format))]]))]))

(defn optional-container [title & children]
  (into [:div.optional-attributes
         [:hr]
         [:span title]]
        children))

(defn preview-panel [loc]
  (let [preview @(subscribe [:exporttable/preview loc])]
    [optional-container "Preview (first 3 rows)"
     (if (nil? preview)
       [:pre [:code.nohighlight "Fetching preview... " [:i.fa.fa-fw.fa-spinner.fa-spin]]]
       [:pre preview])]))

(defn column-headers-panel [loc {:keys [columnheaders]}]
  [optional-container "Column headers"
   [:div.radio
    [:label
     [:input {:type "radio"
              :name "colum-headers-type"
              :on-change #(dispatch [:exporttable/set-column-headers loc nil])
              :checked (nil? columnheaders)}]
     " No column headers"]]
   [:div.radio
    [:label
     [:input {:type "radio"
              :name "colum-headers-type"
              :on-change #(dispatch [:exporttable/set-column-headers loc "friendly"])
              :checked (= columnheaders "friendly")}]
     " Use human readable headers (e.g. " [:em "Gene > Organism Name"] ")"]]
   [:div.radio
    [:label
     [:input {:type "radio"
              :name "colum-headers-type"
              :on-change #(dispatch [:exporttable/set-column-headers loc "path"])
              :checked (= columnheaders "path")}]
     " Use raw path headers (e.g. " [:em "Gene.organism.name"] ")"]]])

(defn rows-panel [loc {:keys [size start]}]
  (let [{total :iTotalRecords} @(subscribe [:main/query-response loc])]
    [optional-container "Select rows"
     [:div
      [:div.form-group
       [:label (str "Size: " size) (when (= size total) " (all rows)")]
       [:input
        {:type "range"
         :name "size"
         :step 1
         :min 1
         :max total
         :value size
         :on-change #(dispatch [:exporttable/set-rows-size loc (js/parseInt (oget % :target :value))])}]]
      [:div.form-group
       [:label (str "Offset: " start)]
       [:input
        {:type "range"
         :name "start"
         :step 1
         :min 0
         :max (dec total)
         :value start
         :on-change #(dispatch [:exporttable/set-rows-start loc (js/parseInt (oget % :target :value))])}]]]]))

(defn data-package-panel [loc {:keys [export-data-package]}]
  [optional-container "Frictionless Data Package"
   [:label
    [:input {:type "checkbox"
             :on-change #(dispatch [:exporttable/toggle-export-data-package loc])
             :checked export-data-package}]
    " Export Frictionless Data Package (uses ZIP compression)"]])

(defn compression-panel [loc {:keys [export-data-package compression]}]
  [optional-container "Compression"
   (when export-data-package
     [:div.alert.alert-warning {:role "alert"}
      [:p "Frictionless Data Package uses ZIP Compression only."]])
   [:div.radio
    [:label
     [:input {:type "radio"
              :name "compression-type"
              :disabled export-data-package
              :on-change #(dispatch [:exporttable/set-compression loc nil])
              :checked (nil? compression)}]
     " No compression"]]
   [:div.radio
    [:label
     [:input {:type "radio"
              :name "compression-type"
              :disabled export-data-package
              :on-change #(dispatch [:exporttable/set-compression loc "zip"])
              :checked (= compression "zip")}]
     " Use Zip compression (produces a .zip archive)"]]
   [:div.radio
    [:label
     [:input {:type "radio"
              :name "compression-type"
              :disabled export-data-package
              :on-change #(dispatch [:exporttable/set-compression loc "gzip"])
              :checked (= compression "gzip")}]
     " Use GZIP compression (produces a .gzip archive)"]]])

(defn modal-body
  [loc]
  (let [{:keys [format] :as data-out} @(subscribe [:settings/data-out loc])]
    [:div.exporttable-body
     [:div.form
      [:div.form-group
       [:label "File name and type"]
       [:div.input-group
        [:input.form-control
         {:type "text"
          :value (:filename data-out)
          :on-change #(dispatch [:exporttable/set-filename loc (oget % :target :value)])}]
        [:div.input-group-btn
         [format-dropdown loc data-out]]]]]
     [:div.export-options
      [preview-panel loc]
      (when (tabular? format)
        [column-headers-panel loc data-out])
      (when-not (bioinf? format)
        [rows-panel loc data-out])
      [data-package-panel loc data-out]
      [compression-panel loc data-out]]]))

(defn modal-footer
  "Clicking the anchor element will cause the file to be downloaded directly
  from the server due to it responding with content-disposition: attachment."
  [loc]
  (let [href @(subscribe [:export/download-href loc])]
    [:a.btn.btn-raised.btn-primary
     {:href href}
     "Download file"]))

(defn export-menu
  "UI element. Presents the modal to allow user to select an export format."
  [loc]
  {:header [:h4 "Export this table as..." [:a.close {:on-click #(dispatch [:modal/close loc])} "x"]]
   :body [modal-body loc]
   :footer [modal-footer loc]})

(defn exporttable [loc]
  [:button.btn.btn-default
   {:type "button"
    :on-click (fn []
                (dispatch [:exporttable/fetch-preview loc])
                (dispatch [:exporttable/prepare-options loc])
                (dispatch [:modal/open loc (export-menu loc)]))}
   [:i.fa.fa-download] " Export"])

