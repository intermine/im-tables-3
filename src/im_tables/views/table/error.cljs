(ns im-tables.views.table.error
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]
            [imcljs.query :refer [->xml]]
            [oops.core :refer [ocall oget]]
            [cljs-time.core :as time]
            [cljs-time.format :refer [unparse formatters]]
            [goog.string :as gstring]))

;; This email is used if no maintainerEmail is available, or when we wish to
;; CC support (ie. when the failure is caused by a software problem).
(def support-email "info@intermine.org")

(defn mailto-string [loc query-xml query-error & {:keys [cc-support?]}]
  (let [maintainer-email (get-in @(subscribe [:settings/settings loc])
                                 [:mine :maintainerEmail]
                                 support-email)
        current-url (oget js/window :location :href)
        service-url (:root @(subscribe [:assets/service loc]))
        current-date (unparse (formatters :rfc822) (time/now))]
    (str "mailto:" maintainer-email
         "?subject=" (gstring/urlEncode "[IMTABLES] - Error running query")
         (when cc-support?
           (str "&cc=" support-email))
         "&body=" (gstring/urlEncode
                    (str "We encountered an error running a query from an embedded result table.

page: " current-url "
service: " service-url "
date: " current-date "
-------------------------------
QUERY: " query-xml "
-------------------------------
ERROR: " query-error)))))

(defn no-results [loc]
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

(defn query-failure
  "Failure message for invalid query."
  [{:keys [loc
           show-query? show-error?
           toggle-query toggle-error
           mailto
           query-xml query-error]}]
  [:div.alert.alert-warning.table-error {:role "alert"}
   [:h2 [:i.fa.fa-bug] " Invalid query"]
   [:p "The server response indicates that there is something wrong with the query XML. You will likely have to modify the query to fix this error. The easiest way to do this would be to return to the query builder (using your browser's back button) but you can also fix it using the controls above."]
   [:p "If you did not make this query (i.e. it's a public list, template or part of a report page) or you believe there is something wrong on the server end, please use the button below to send an email with a pre-filled bug report to the maintainers."]
   [:div.button-group
    [:button.btn.btn-default
     {:type "button"
      :on-click toggle-query
      :class (when show-query? "active")}
     [:i.fa.fa-code] " Show query"]
    [:button.btn.btn-default
     {:type "button"
      :on-click toggle-error
      :class (when show-error? "active")}
     [:i.fa.fa-bug] " Show error"]
    [:a.btn.btn-primary.pull-right
     {:href mailto}
     [:i.fa.fa-envelope] " Send a bug report"]]
   (when show-query?
     [:pre.well [code-block loc query-xml]])
   (when show-error?
     [:pre.well.text-danger query-error])])

(defn server-failure
  "Failure message for server error."
  [{:keys [loc
           show-query?
           toggle-query
           mailto
           query-xml]}]
  [:div.alert.alert-warning.table-error {:role "alert"}
   [:h2 [:i.fa.fa-bug] " Server error"]
   [:p "You have received an invalid response from the server. This can be a sign of network issues or problems with the server itself. If the issue persists, please use the button below to send an email with a pre-filled bug report to the maintainers."]
   [:div.button-group
    (when (not-empty query-xml)
      [:button.btn.btn-default
       {:type "button"
        :on-click toggle-query
        :class (when show-query? "active")}
       [:i.fa.fa-code] " Show query"])
    [:button.btn.btn-info
     {:type "button"
      :on-click #(dispatch [:main/retry-failure loc])}
     [:i.fa.fa-refresh] " Retry"]
    [:a.btn.btn-primary.pull-right
     {:href mailto}
     [:i.fa.fa-envelope] " Send a bug report"]]
   (when show-query?
     [:pre.well [code-block loc query-xml]])])

(defn boundary-failure
  "Failure message when catching uncaught error with Error Boundary."
  [{:keys [loc
           show-error?
           toggle-error
           mailto
           error
           clear-error!]}]
  [:div.alert.alert-warning.table-error {:role "alert"}
   [:h2 [:i.fa.fa-bug] " im-tables crashed"]
   [:p "Something unexpected happened that im-tables wasn't able to handle. If you have a moment, we would appreciate it if you used the button below to send an email with a pre-filled bug report to the maintainers. Any description of what you were doing before this happened would be very helpful."]
   [:div.button-group
    (when (not-empty error)
      [:button.btn.btn-default
       {:type "button"
        :on-click toggle-error
        :class (when show-error? "active")}
       [:i.fa.fa-code] " Show error"])
    [:button.btn.btn-info
     {:type "button"
      :on-click clear-error!}
     [:i.fa.fa-refresh] " Restart"]
    [:a.btn.btn-primary.pull-right
     {:href mailto}
     [:i.fa.fa-envelope] " Send a bug report"]]
   (when show-error?
     [:pre.well.text-danger error])])

(defn failure [loc error]
  (let [show-query (reagent/atom false)
        show-error (reagent/atom false)]
    (fn [loc {error-type :type :as error} & {:keys [clear-error!]}]
      (let [query-xml (when-let [model @(subscribe [:assets/model loc])]
                        (->xml model @(subscribe [:main/query loc])))
            toggle-query #(swap! show-query not)
            toggle-error #(swap! show-error not)]
        (case error-type
          :query [query-failure
                  {:loc loc
                   :show-query? @show-query
                   :show-error? @show-error
                   :toggle-query toggle-query
                   :toggle-error toggle-error
                   :mailto (mailto-string loc query-xml (:message error))
                   :query-xml query-xml
                   :query-error (:message error)}]
          :network [server-failure
                    {:loc loc
                     :show-query? @show-query
                     :toggle-query toggle-query
                     :mailto (mailto-string loc query-xml (:message error))
                     :query-xml query-xml}]
          :boundary (let [error-string (str (:error error)
                                            \newline
                                            (or (oget (:info error) :componentStack)
                                                (ocall js/JSON :stringify (:info error))))]
                      [boundary-failure
                       {:loc loc
                        :show-error? @show-error
                        :toggle-error toggle-error
                        :mailto (mailto-string loc query-xml error-string :cc-support? true)
                        :error error-string
                        :clear-error! clear-error!}]))))))
