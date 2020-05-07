(ns im-tables.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [cljs.core.async :refer [<! timeout close!]]))

(defn im-operation
  [{:keys [on-success on-failure response-format op channel params]}]
  (go
    (let [{:keys [statusCode status] :as response} (<! (or channel (op)))
          ;; `statusCode` is part of the response body from InterMine.
          ;; `status` is part of the response map created by cljs-http.
          s (or statusCode status)
          ;; Response can be nil or "" when offline.
          valid-response? (and (some? response)
                               (not= response ""))]
      ;; Note that `s` can be nil for successful responses, due to
      ;; imcljs applying a transducer on success. The proper way to
      ;; check for null responses (which don't have a status code)
      ;; is to check if the response itself is nil.
      (cond
        ;; This first clause will intentionally match on s=nil.
        (and valid-response?
             (< s 400)) (dispatch (conj on-success response))
        on-failure (dispatch (conj on-failure response))
        :else
        (do ;; I commented this out since in some cases, like fetching a
            ;; column summary, you don't want the table to be replaced with
            ;; an error message should it fail. Instead we need a less
            ;; intrusive way of indicating failure for these cases, while
            ;; `:error/response` should be reserved for when a query or its
            ;; dependents fail.
            #_(when-let [loc (second on-success)]
                ;; We are being sneaky in taking advantage of `loc` always being
                ;; passed as the first event handler argument. If we find this
                ;; fallback error event useful, we should require that all uses
                ;; of this effect pass a `loc` key.
                (when (sequential? loc)
                  ;; Dispatch generic network error event if no `on-failure` defined.
                  (dispatch [:error/response loc response])))
            (.error js/console "Failed imcljs request" response))))))

(reg-fx
 :im-tables/im-operation
 (fn [op-map]
   (im-operation op-map)))

(reg-fx
 :im-tables/im-operation-chan
 (fn [op-map]
   (im-operation op-map)))

;; Currently this only logs to console, but in the future we can decide to
;; include more information and perhaps log to a reporting service.
(reg-fx
 :im-tables/log-error
 (fn [[error-string data-map]]
   (.error js/console
           (str "im-tables: " error-string)
           (clj->js data-map))))
