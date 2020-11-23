(ns im-tables.events.pagination
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [im-tables.interceptors :refer [sandbox]]))

(reg-event-fx
 :imt.pagination/check-for-results
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [{:keys [start limit] :as p} (get-in db [:settings :pagination])
         fetch-more? (not-every? (fn [n] (contains? (get-in db [:response :results]) n)) (range start (+ start limit)))]
     (cond-> {:db db}
       fetch-more? (assoc :dispatch [:im-tables.main/run-query loc true])))))

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
    ; Make sure that never dec to a negative number
    ; (This could happen if a user goes to page 2 with a limit of 10, changes limit to 20, then decs (-10)
   {:db (update-in db [:settings :pagination :start] (comp (partial max 0) #(- % %2)) (get-in db [:settings :pagination :limit]))
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
   (let [total (get-in db [:response :iTotalRecords])
         limit (get-in db [:settings :pagination :limit])
         remaining (mod total limit)
         start (- total (if (zero? remaining) limit remaining))]
     {:db (assoc-in db [:settings :pagination :start] start)
      :dispatch [:imt.pagination/check-for-results loc]})))

(reg-event-fx
 :imt.settings/update-pagination-limit
 (sandbox)
 (fn [{db :db} [_ loc limit]]
   {:db       (assoc-in db [:settings :pagination :limit] limit)
    :dispatch [:imt.pagination/check-for-results loc]}))
