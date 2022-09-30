(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join replace]]
            [imcljs.fetch :as fetch]
            [im-tables.interceptors :refer [sandbox]]))

;; REMEMBER KIDS, some gene identifiers have a comma in them, because insanity.
;; This means we default to tsv for Good Reasons. (This is set in the app-db!)

(reg-event-db
 :exporttable/prepare-options
 (sandbox)
 (fn [db [_ loc]]
   (assoc-in db [:settings :data-out :filename]
             (str (when-let [mine-ns (not-empty (get-in db [:settings :links :vocab :mine]))]
                    (str mine-ns "_"))
                  "results_"
                  ;; This creates a filename friendly date and time string.
                  (let [date (js/Date.)]
                    ;; Adjust for the timezone and daylight saving components, so we can convert to ISO format without displaying zero UTC time.
                    (.setMinutes date (- (.getMinutes date) (.getTimezoneOffset date)))
                    (-> (.toISOString date)
                        ;; Remove the millisecond counter and Z suffix, as we removed the UTC offset above.
                        (replace #"\..*$" "")
                        ;; Remove the : character as it's not supported in most filesystems.
                        (replace #":" "")))))))

(reg-event-fx
 :exporttable/fetch-preview
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [options (merge
                   (select-keys (get-in db [:settings :data-out])
                                [:format :columnheaders])
                   {:start 0
                    :size 3})]
     (if (contains? #{"fasta" "gff3" "bed"} (:format options))
       {:db (assoc-in db [:cache :export-preview] "Previews are not supported for bioinformatics formats")}
       {:db (assoc-in db [:cache :export-preview] nil)
        :im-tables/im-operation-chan {:channel (fetch/fetch-custom-format (:service db) (:query db) options)
                                      :on-success [:exporttable/fetch-preview-success loc]
                                      :on-failure [:exporttable/fetch-preview-failure loc]}}))))

(reg-event-db
 :exporttable/fetch-preview-success
 (sandbox)
 (fn [db [_ loc res]]
   (assoc-in db [:cache :export-preview] (if (coll? res) (.stringify js/JSON (clj->js res) nil 4) res))))

(reg-event-db
 :exporttable/fetch-preview-failure
 (sandbox)
 (fn [db [_ loc res]]
   (assoc-in db [:cache :export-preview] (or (not-empty (:body res))
                                             "Failed to fetch preview"))))

(reg-event-db
 :exporttable/set-filename
 (sandbox)
 (fn [db [_ loc filename]]
   (assoc-in db [:settings :data-out :filename] filename)))

(reg-event-fx
 :exporttable/set-format
 ;;sets preferred format for the file export
 (sandbox)
 (fn [{db :db} [_ loc format]]
   {:db (assoc-in db [:settings :data-out :format] format)
    :dispatch [:exporttable/fetch-preview loc]}))

(reg-event-fx
 :exporttable/set-column-headers
 (sandbox)
 (fn [{db :db} [_ loc colum-headers-type]]
   {:db (assoc-in db [:settings :data-out :columnheaders] colum-headers-type)
    :dispatch [:exporttable/fetch-preview loc]}))

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
