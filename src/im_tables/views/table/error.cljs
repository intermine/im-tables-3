(ns im-tables.views.table.error
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as reagent]
            [imcljs.query :refer [->xml]]
            [oops.core :refer [ocall oget]]
            [cljs-time.core :as time]
            [cljs-time.format :refer [unparse formatters]]
            [goog.string :as gstring]))

(defn mailto-string [loc query-xml query-error]
  (let [maintainer-email (get-in @(subscribe [:settings/settings loc])
                                 [:mine :maintainerEmail]
                                 "info@intermine.org")
        current-url (oget js/window :location :href)
        service-url (:root @(subscribe [:assets/service loc]))
        current-date (unparse (formatters :rfc822) (time/now))]
    (str "mailto:" maintainer-email
         "?subject=" (gstring/urlEncode "[IMTABLES] - Error running query")
         "&body=" (gstring/urlEncode
                    (str "We encountered an error running a query from an embedded result table.

page: " current-url "
service: " service-url "
date: " current-date "
-------------------------------
QUERY: " query-xml "
-------------------------------
ERROR: " query-error)))))

(defn no-results [loc res]
  [:div.alert.alert-info {:role "alert"}
   [:h2 "No results"]])

(defn code-block [loc code-string]
  (let [!code (atom nil)]
    (reagent/create-class
     {:component-did-mount (fn [_]
                             (when-let [code-elem @!code]
                               (ocall js/hljs :highlightBlock code-elem)))
      :reagent-render (fn [loc code-string]
                        [:code.language-xml
                         {:ref #(reset! !code %)}
                         code-string])})))

(defn failure [loc res]
  (let [show-query (reagent/atom false)
        show-error (reagent/atom false)]
    (fn [loc res]
      (let [model @(subscribe [:assets/model loc])
            query-xml (->xml model @(subscribe [:main/query loc]))
            query-error (get-in res [:body :error])]
        [:div.alert.alert-warning.table-error {:role "alert"}
         (if query-error
           [:<>
            [:h2 [:i.fa.fa-bug] " Invalid query"]
            [:p "The server response indicates that there is something wrong with the query XML. You will likely have to modify the query to fix this error. If you believe there is something wrong on the server end, please use the button below to send an email with a pre-filled bug report to the maintainers."]
            [:div.button-group
             [:button.btn.btn-default
              {:type "button"
               :on-click #(swap! show-query not)
               :class (when @show-query "active")}
              [:i.fa.fa-code] " Show query"]
             [:button.btn.btn-default
              {:type "button"
               :on-click #(swap! show-error not)
               :class (when @show-error "active")}
              [:i.fa.fa-bug] " Show error"]
             [:a.btn.btn-primary.pull-right
              {:href (mailto-string loc query-xml query-error)}
              [:i.fa.fa-envelope] " Send a bug report"]]
            (when @show-query
              [:pre.well [code-block loc query-xml]])
            (when @show-error
              [:pre.well.text-danger query-error])]
           [:<>
            [:h2 [:i.fa.fa-bug] " Server error"]
            [:p "You have received an invalid response from the server. This can be a sign of network issues or problems with the server itself. If the issue persists, please use the button below to send an email with a pre-filled bug report to the maintainers."]
            [:div.button-group
             [:button.btn.btn-default
              {:type "button"
               :on-click #(swap! show-query not)
               :class (when @show-query "active")}
              [:i.fa.fa-code] " Show query"]
             [:a.btn.btn-primary.pull-right
              {:href (mailto-string loc query-xml query-error)}
              [:i.fa.fa-envelope] " Send a bug report"]]
            (when @show-query
              [:pre.well [code-block loc query-xml]])])]))))
