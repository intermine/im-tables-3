(ns im-tables.events.pagination
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [im-tables.interceptors :refer [sandbox]]))

(reg-event-fx
  :imt.pagination/check-for-results
  (sandbox)
  (fn [{db :db} [_ loc]]
    (let [{:keys [start limit] :as p} (get-in db [:settings :pagination])
          fetch-more? (not-every? (fn [n] (contains? (get-in db [:query-response :results]) n)) (range start (+ start limit)))]
      (println "TEST" (map (fn [n] (contains? (get-in db [:query-response :results]) n)) (range start (+ start limit))))
      (cond-> {:db db}
              fetch-more? (assoc :dispatch [:im-tables.main/run-query loc])))))

(reg-event-fx
  :imt.settings/update-pagination-inc
  (sandbox)
  (fn [{db :db} [_ loc]]
    {:db       (update-in db [:settings :pagination :start] + (get-in db [:settings :pagination :limit]))
     :dispatch [:imt.pagination/check-for-results loc]}))

(reg-event-fx
  :imt.settings/update-pagination-dec
  (sandbox)
  (fn [{db :db} [_ loc]]
    {:db       (update-in db [:settings :pagination :start] - (get-in db [:settings :pagination :limit]))
     :dispatch [:imt.pagination/check-for-results loc]}))

(reg-event-fx
  :imt.settings/update-pagination-fulldec
  (sandbox)
  (fn [{db :db} [_ loc]]
    {:db       (assoc-in db [:settings :pagination :start] 0)
     :dispatch [:imt.pagination/check-for-results loc]}))

(reg-event-fx
  :imt.settings/update-pagination-fullinc
  (sandbox)
  (fn [{db :db} [_ loc]]
    (let [total (get-in db [:query-response :iTotalRecords])]
      {:db       (assoc-in db [:settings :pagination :start] (- total (mod total 10)))
       :dispatch [:imt.pagination/check-for-results loc]})))