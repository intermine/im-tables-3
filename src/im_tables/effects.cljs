(ns im-tables.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [cljs.core.async :refer [<! timeout close!]]))

(reg-fx
 :im-tables/im-operation
 (fn [{:keys [on-success on-failure response-format op params]}]
   (go (dispatch (conj on-success (<! (op)))))))

(reg-fx
 :im-tables/im-operation-chan
 (fn [{:keys [on-success on-failure response-format channel params]}]
   (go
     (let [{:keys [status] :as response} (<! channel)]
       (cond
         (< status 400) (dispatch (conj on-success response))
         :else (dispatch (conj (or on-failure on-success) response)))))))
