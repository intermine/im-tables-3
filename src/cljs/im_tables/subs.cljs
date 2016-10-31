(ns im-tables.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(reg-sub
  :main/query-response
  (fn [db]
    (get db :query-response)))

(reg-sub
  :summary/item-summary
  (fn [db [_ id]]
    (get-in db [:cache :summaries id])))

(reg-sub
  :settings/pagination
  (fn [db]
    (get-in db [:settings :pagination])))
