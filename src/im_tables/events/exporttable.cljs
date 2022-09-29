(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join]]
            [imcljs.fetch :as fetch]
            [im-tables.interceptors :refer [sandbox]]))

;; REMEMBER KIDS, some gene identifiers have a comma in them, because insanity.
;; This means we default to tsv for Good Reasons. (This is set in the app-db!)

(reg-event-db
 :exporttable/set-format
 ;;sets preferred format for the file export
 (sandbox)
 (fn [db [_ loc format]]
   (assoc-in db [:settings :data-out :selected-format] (keyword format))))

(reg-event-db
 :exporttable/toggle-export-data-package
 (sandbox)
 (fn [db [_ loc]]
   (update-in db [:settings :data-out :export-data-package] not)))

(reg-event-db
 :exporttable/set-compression
 (sandbox)
 (fn [db [_ loc compression-type]]
   (assoc-in db [:settings :data-out :compression] compression-type)))

(reg-event-db
 :exporttable/set-column-headers
 (sandbox)
 (fn [db [_ loc colum-headers-type]]
   (assoc-in db [:settings :data-out :column-headers] colum-headers-type)))
