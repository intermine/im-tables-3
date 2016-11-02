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
  :main/query
  (fn [db]
    (get db :query)))

(reg-sub
  :summary/item-details
  (fn [db [_ id]]
    (get-in db [:cache :item-details id])))

(reg-sub
  :style/dragging-item
  (fn [db [_]]
    (get-in db [:cache :dragging-item])))

(reg-sub
  :style/dragging-over
  (fn [db [_]]
    (get-in db [:cache :dragging-over])))

(reg-sub
  :settings/pagination
  (fn [db]
    (get-in db [:settings :pagination])))

(reg-sub
  :summaries/column-summaries
  (fn [db]
    (get-in db [:cache :column-summary])))

(reg-sub
  :selection/selections
  (fn [db [_ view]]
    (get-in db [:cache :column-summary view :selections])))

(reg-sub
  :selection/response
  (fn [db [_ view]]
    (get-in db [:cache :column-summary view :response])))

(reg-sub
  :selection/text-filter
  (fn [db [_ view]]
    (get-in db [:cache :column-summary view :filters :text])))

(reg-sub
  :assets/model
  (fn [db]
    (get-in db [:assets :model])))

(reg-sub
  :tree-view/selection
  (fn [db]
    (get-in db [:cache :tree-view :selection])))
