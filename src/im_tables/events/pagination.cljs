(ns im-tables.events.pagination
  (:require [re-frame.core :refer [reg-event-db]]
            [im-tables.interceptors :refer [sandbox]]))

(reg-event-db
  :imt.settings/update-pagination-inc
  (sandbox)
  (fn [db [_ loc]]
    (update-in db [:settings :pagination :start] + (get-in db [:settings :pagination :limit]))))

(reg-event-db
  :imt.settings/update-pagination-dec
  (sandbox)
  (fn [db [_ loc]]
    (update-in db [:settings :pagination :start] - (get-in db [:settings :pagination :limit]))))

(reg-event-db
  :imt.settings/update-pagination-fulldec
  (sandbox)
  (fn [db [_ loc]]
    (assoc-in db [:settings :pagination :start] 0)))

(reg-event-db
  :imt.settings/update-pagination-fullinc
  (sandbox)
  (fn [db [_ loc]]
    (let [total (get-in db [:query-response :iTotalRecords])]
      (assoc-in db [:settings :pagination :start] (- total (mod total 10))))))