(ns im-tables.views.dashboard.manager.codegen.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            ["highlight.js" :as hljs]
            [clojure.string :as string]
            [imcljs.internal.utils :refer [scrub-url]]
            [oops.core :refer [ocall oget]]))


(def languages {"js"   {:label "JavaScript"}
                "pl"   {:label "Perl"}
                "py"   {:label   "Python"
                        :comment "#"}
                "rb"   {:label   "Ruby"
                        :comment "#"}
                "java" {:label "Java"}})

(defn generate-javascript [{:keys [cdn query service html? comments?]}]
  (string/join "\n"
               (-> []
                   (cond->
                     (and html? comments?) (conj "<!-- The Element we will target -->")
                     html? (conj "<div id=\"some-elem\"></div>\n")
                     (and html? comments?) (conj "<!-- The imtables source -->")
                     html? (conj (str "<script src=\"" cdn "/js/intermine/im-tables/2.0.0-beta/imtables.js\" charset=\"UTF8\"></script>"))
                     html? (conj (str "<link href=\"" cdn "/js/intermine/im-tables/2.0.0-beta/main.sandboxed.css\" rel=\"stylesheet\">\n"))
                     html? (conj "<script>")
                     html? (conj "var selector = \"#some-elem\";\n")
                     (and (not html?) comments?) (conj "/* Install from npm: npm install imtables\n * This snippet assumes the presence on the page of an element like:\n * <div id=\"some-elem\"></div>\n */")
                     (not html?) (conj "var imtables = require(\"imtables\");\n"))
                   (conj (str "var service = " (js/JSON.stringify
                                                 (clj->js (-> service
                                                              (select-keys [:root :token])
                                                              (update :root scrub-url))) nil 2) "\n"))
                   (conj (str "var query = " (js/JSON.stringify (clj->js query) nil 2) "\n"))
                   (conj (str "imtables.loadTable(\n  selector, // Can also be an element, or a jQuery object.\n  {\"start\":0,\"size\":25}, // May be null\n  {service: service, query: query} // May be an imjs.Query\n).then(\n  function (table) { console.log('Table loaded', table); },\n  function (error) { console.error('Could not load table', error); }\n);"))
                   (cond->
                     html? (conj "</script>")))))

(defn options []
  (fn [loc options-atom on-change-lang]
    (let [{:keys [html? comments?]} @options-atom
          lang @(subscribe [:codegen/lang loc])]
      [:div
       [:select.form-control
        {:value     lang
         :on-change (fn [e]
                      (swap! options-atom assoc :lang (oget e :target :value))
                      (on-change-lang (oget e :target :value)))}
        (map (fn [[value {:keys [label]}]] ^{:key value} [:option {:value value} label]) languages)]
       (when (= "js" lang)
         [:div.checkbox
          [:label [:input
                   {:type      "checkbox"
                    :value     ""
                    :on-change (fn [e] (swap! options-atom update :html? not))
                    :checked   html?}] "Include HTML"]])
       [:div.checkbox
        [:label [:input
                 {:type      "checkbox"
                  :value     ""
                  :on-change (fn [e] (swap! options-atom update :comments? not))
                  :checked   comments?}] "Include comments"]]])))

(def doesnt-start-with (complement clojure.string/starts-with?))

(defn remove-java-comments [s]
  (clojure.string/replace s #"/\*([\S\s]*?)\*/" ""))

(defn octo-comment? [s]
  (or
    (clojure.string/starts-with? s "# ")
    (every? true? (map (partial = "#") s))))

(def not-octo-comment? (complement octo-comment?))

(defn remove-octothorpe-comments [s]
  (let [lines (clojure.string/split-lines s)]
    (->> lines
         (filter not-octo-comment?)
         (clojure.string/join "\n"))))

(defn modal-body [loc]
  (let [code           (subscribe [:codegen/code loc])
        codelang       (subscribe [:codegen/lang loc])
        rel-query      (subscribe [:rel-manager/query loc])
        service        (subscribe [:assets/service loc])
        query          (subscribe [:main/query loc])
        settings       (subscribe [:settings/settings loc])
        options-atom   (r/atom {:lang "js" :comments? true})
        on-change-lang (fn [lang]
                         (dispatch [:main/generate-code loc @service (:model @service) @query lang]))]
    (r/create-class
      {:component-did-mount  (fn [this]
                               (ocall (js/$ "pre code") :each
                                      (fn [i block]
                                        (ocall hljs :highlightBlock block))))
       :component-did-update (fn [this]
                               (ocall (js/$ "pre code") :each
                                      (fn [i block]
                                        (ocall hljs :highlightBlock block))))
       :reagent-render       (fn [loc]
                               [:div.container-fluid
                                [:div.row
                                 [:div.col-xs-3 [options loc options-atom on-change-lang]]
                                 [:div.col-xs-9
                                  (if (nil? @code)
                                    [:pre [:code.nohighlight "Generating code... " [:i.fa.fa-fw.fa-spinner.fa-spin]]]
                                    (cond
                                     (= "js" @codelang) (when (and @query @service)
                                                          [:pre [:code (generate-javascript {:query     @query
                                                                                             :service   @service
                                                                                             :comments? (:comments? @options-atom)
                                                                                             :html?     (:html? @options-atom)
                                                                                             :cdn       (:cdn @settings)})]])
                                     (= "java" @codelang) [:pre [:code (cond-> @code (not (:comments? @options-atom)) remove-java-comments)]]
                                     (or
                                       (= "rb" @codelang)
                                       (= "py" @codelang)
                                       (= "pl" @codelang)) [:pre [:code (cond-> @code (not (:comments? @options-atom)) remove-octothorpe-comments)]]
                                     :else [:pre [:code @code]]
                                     ))]]
                                ])})))

(defn modal-footer [loc]
  (let [model (subscribe [:assets/model loc])]
    (fn [loc]
      [:div.btn-toolbar.pull-right
       [:button.btn.btn-default {:on-click (fn [] (dispatch [:prep-modal loc nil]))} "Cancel"]
       [:button.btn.btn-success
        {:on-click (fn []
                     ; Apply the changes
                     (dispatch [:rel-manager/apply-changes loc])
                     ; Close the modal by clearing the markup from app-db
                     (dispatch [:prep-modal loc nil]))}
        "Apply Changes"]])))

(defn build-modal [loc]
  {:header [:h3 "Generate Code"]
   :body   [modal-body loc]
   :footer [modal-footer loc]})

(defn main [loc]
  (let [query   (subscribe [:main/query loc])
        service (subscribe [:assets/service loc])
        lang (subscribe [:codegen/lang loc])]
    (fn [loc]
      [:div
       [:div.btn-group
        [:button.btn.btn-default
         {:on-click (fn []
                      (dispatch [:prep-modal loc (build-modal loc)])
                      (dispatch [:main/generate-code loc @service (:model @service) @query @lang]))}
         [:i.fa.fa-share-alt] (str " " (get-in languages [@lang :label]))]
        [:button.btn.btn-default.dropdown-toggle
         {:data-toggle "dropdown"} [:span.caret]]
        (into [:ul.dropdown-menu]
              (map (fn [[value {:keys [label]}]]
                     [:li {:on-click (fn []
                                       (dispatch [:main/generate-code loc @service (:model @service) @query value])
                                       (dispatch [:prep-modal loc (build-modal loc)]))}
                      [:a label]]) languages))]])))