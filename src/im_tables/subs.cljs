(ns im-tables.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(defn glue [path remainder-vec]
  (reduce conj (or path []) remainder-vec))

(reg-sub
  :main/query-response
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query-response]))))

(reg-sub
  :main/query
  (fn [db [_ prefix]]
    (get db :query)))

(reg-sub
  :main/temp-query
  (fn [db [_ prefix]]
    (get db :temp-query)))

(reg-sub
  :main/query-parts
  (fn [db [_ prefix]]
    (get db :query-parts)))

(reg-sub
  :style/overlay?
  (fn [db [_ prefix]]
    ;(println "GOT LOC" loc )
    ;(println "GLUED" (get-in db (glue loc [:cache :overlay?])))
    (get-in db (glue prefix [:cache :overlay?]))))

(reg-sub
  :summary/item-details
  (fn [db [_ prefix id]]
    (get-in db [:cache :item-details id])))

(reg-sub
  :style/dragging-item
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :dragging-item]))))

(reg-sub
  :style/dragging-over
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :dragging-over]))))

(reg-sub
  :settings/pagination
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:settings :pagination]))))

(reg-sub
  :summaries/column-summaries
  (fn [db [_ prefix]]
    (get-in db [:cache :column-summary])))

(reg-sub
  :selection/selections
  (fn [db [_ prefix view]]
    (get-in db [:cache :column-summary view :selections])))

(reg-sub
  :selection/response
  (fn [db [_ prefix view]]
    (get-in db [:cache :column-summary view :response])))

(reg-sub
  :selection/text-filter
  (fn [db [_ prefix view]]
    (get-in db [:cache :column-summary view :filters :text])))

(reg-sub
  :assets/model
  (fn [db [_ prefix]]
    (get-in db [:assets :model :classes])))

(reg-sub
  :tree-view/selection
  (fn [db [_ prefix]]
    (get-in db [:cache :tree-view :selection])))

(reg-sub
  :modal
  (fn [db [_ prefix]]
    (get-in db [:cache :modal])))