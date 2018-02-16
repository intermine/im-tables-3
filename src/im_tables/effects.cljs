(ns im-tables.effects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [cljs.core.async :refer [<! timeout close!]]))

(reg-fx
  :im-tables/im-operation
  (fn [{:keys [on-success on-failure response-format op params]}]
    (go (dispatch (conj on-success (<! (op)))))))

(reg-fx
  :im-tables/im-operation-channel
  (fn [{:keys [on-success on-failure response-format channel params]}]
    (go (dispatch (conj on-success (<! channel))))))