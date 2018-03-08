(ns im-tables.views.dashboard.manager.codegen.main
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            ["highlight.js" :as hljs]
            [oops.core :refer [ocall]]))


(defn modal-body [loc]
  (let [code      (subscribe [:codegen/code loc])
        rel-query (subscribe [:rel-manager/query loc])]
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
                               [:div
                                [:pre [:code (str @code)]]])})))

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
        service (subscribe [:assets/service loc])]
    (fn [loc]
      [:div
       [:div.btn-group
        [:button.btn.btn-default
         {:on-click (fn []
                      ; Reset the state of the modal
                      #_(dispatch [:rel-manager/reset loc])
                      ; Build the modal markup and send it to app-db
                      (dispatch [:prep-modal loc (build-modal loc)])
                      (dispatch [:main/generate-code loc @service (:model @service) @query]))}
         [:i.fa.fa-share-alt] " Generate Code"]]])))