(ns im-tables.views.dashboard.manager.codegen.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            ["highlight.js" :as hljs]
            [clojure.string :as string]
            [imcljs.internal.utils :refer [scrub-url]]
            [oops.core :refer [ocall oget oset!]]))

(def languages {"js" {:label "JavaScript"}
                "pl" {:label "Perl"}
                "py" {:label "Python"
                      :comment "#"}
                "rb" {:label "Ruby"
                      :comment "#"}
                "java" {:label "Java"}})

(defn options [loc]
  (let [options (subscribe [:codegen/options loc])]
    (fn [loc]
      (let [{:keys [html? comments? lang]} @options]
        [:div
         [:pre (str @options)]
         [:select.form-control
          {:value lang
           :on-change (fn [e]
                        (dispatch [:main/set-codegen-option loc :lang (oget e :target :value) true]))}
          (map (fn [[value {:keys [label]}]] ^{:key value} [:option {:value value} label]) languages)]
         (when (= "js" lang)
           [:div.checkbox
            [:label [:input
                     {:type "checkbox"
                      :value ""
                      :on-change (fn [e] (dispatch [:main/set-codegen-option loc :html? (not html?)]))
                      :checked html?}] "Include HTML"]])
         [:div.checkbox
          [:label [:input
                   {:type "checkbox"
                    :value ""
                    :on-change (fn [e] (dispatch [:main/set-codegen-option loc :comments? (not comments?)]))
                    :checked comments?}] "Include comments"]]]))))


(defn save-to-disk [filename text lang]
  (let [blob (js/Blob. [text] #js {:type "text/plain;charset=utf8"})
        url (-> (ocall js/window :URL.createObjectURL blob))]
    (let [element (ocall js/document :createElement "a")]
      (ocall element :setAttribute "href" url)
      (ocall element :setAttribute "download" (str filename "." lang))
      (oset! element :style :display "none")
      (ocall (oget js/document :body) :appendChild element)
      (ocall element :click)
      (ocall (oget js/document :body) :removeChild element))))

(defn modal-body [loc]
  (let [code (subscribe [:codegen/formatted-code loc])]
    (r/create-class
      {:component-did-mount (fn [this]
                              (ocall (js/$ "pre code") :each
                                     (fn [i block]
                                       (ocall hljs :highlightBlock block))))
       :component-did-update (fn [this]
                               (ocall (js/$ "pre code") :each
                                      (fn [i block]
                                        (ocall hljs :highlightBlock block))))
       :reagent-render (fn [loc]
                         [:div.container-fluid
                          [:div.row
                           [:div.col-xs-3 [options loc]]
                           [:div.col-xs-9
                            (if (nil? @code)
                              [:pre [:code.nohighlight "Generating code... " [:i.fa.fa-fw.fa-spinner.fa-spin]]]
                              [:pre [:code @code]])]]])})))

(defn modal-footer [loc]
  (let [code (subscribe [:codegen/formatted-code loc])
        options (subscribe [:codegen/options loc])]
    (fn [loc]
      [:div.btn-toolbar.pull-right
       [:button.btn.btn-default {:on-click (fn [] (dispatch [:prep-modal loc nil]))} "Close"]
       [:button.btn.btn-primary
        {:on-click (fn [] (save-to-disk "query" @code (:lang @options)))}
        [:i.fa.fa-save] " Download"]])))

(defn build-modal [loc]
  {:header [:h3 "Generate Code"]
   :body [modal-body loc]
   :footer [modal-footer loc]})

(defn main [loc]
  (let [query (subscribe [:main/query loc])
        service (subscribe [:assets/service loc])
        options (subscribe [:codegen/options loc])]
    (fn [loc]
      [:div
       [:div.btn-group
        [:button.btn.btn-default
         {:on-click (fn []
                      (dispatch [:prep-modal loc (build-modal loc)])
                      (dispatch [:main/generate-code loc @service (:model @service) @query (:lang @options)]))}
         [:i.fa.fa-code] (str " " (get-in languages [(:lang @options) :label]))]
        [:button.btn.btn-default.dropdown-toggle
         {:data-toggle "dropdown"} [:span.caret]]
        (into [:ul.dropdown-menu]
              (map (fn [[value {:keys [label]}]]
                     [:li {:on-click (fn []
                                       (dispatch [:main/set-codegen-option loc :lang value true])
                                       (dispatch [:prep-modal loc (build-modal loc)]))}
                      [:a label]]) languages))]])))