(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join]]
            [imcljs.fetch :as fetch]
            [im-tables.interceptors :refer [sandbox]]))

;; REMEMBER KIDS, some gene identifiers have a comma in them, because insanity.
;; This means we default to tsv for Good Reasons. (This is set in the app-db!)

(defn encode-file
  "Encode a stringified text file such that it can be downloaded by the browser.
  Results must be stringified - don't pass objects / vectors / arrays / whatever."
  [data filetype]
  (ocall js/URL "createObjectURL" (js/Blob. (clj->js [data]) {:type (str "text/" filetype)})))

(defn stringify-query-results
  "converts results into a csv/tsv-style string."
  [file-type query-results]
  (let [separator (:separator file-type)
        vec-file
        (reduce (fn [new-str [i rowvals]]
                  (conj new-str
                        (join separator (reduce (fn [new-sub-str rowval]
                                                  (conj new-sub-str (:value rowval))) [] rowvals)))) [] query-results)]
    (join "\n" vec-file)))

;;config for various file types
;;probably should be abstracted to somewhere else more central
(def xsv {:csv {:file-type "csv" :separator ","}
          :tsv {:file-type "tsv" :separator "\t"}
          :fasta {:file-type "fasta"}})

(reg-event-db
 :exporttable/set-format
 ;;sets preferred format for the file export
 (sandbox)
 (fn [db [_ loc format]]
   (assoc-in db [:settings :data-out :selected-format] (keyword format))))

(reg-event-fx
 :exporttable/download
 ;;the main action to download files. This gets called by the download modal button.
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [query-results (get-in db [:response :results])
         file-type ((get-in db [:settings :data-out :selected-format]) xsv)
         query (get-in db [:query])]
     (if (= (:file-type file-type) "fasta")
       ;;fasta queries need to have only one column selected, so have slightly
       ;; differetnt conditions
       {:db db :dispatch [:exporttable/run-fasta-query loc file-type]}
       {:db db :dispatch [:exporttable/run-export-query loc file-type]}))))

(defn set-download-link-properties
  "We're setting the attributes of a link with the download property enabled,
   then clicking it programatically.
   This spawns a nice download without having to create a new window
   which might be pop-up blocked. It also allows us to set the filename.
   We live in the future!"
  [results file-type]
  (let [downloadlink (ocall js/document :getElementById "hiddendownloadlink")]
    (ocall downloadlink :setAttribute "href" (encode-file results file-type))
    (ocall downloadlink :setAttribute "download" (str "results." file-type))
    (ocall downloadlink :click)))

(reg-event-fx
 :exporttable/download-export-response
 ;;fx event handler for downloading the file once the query is complete
 (sandbox)
 (fn [{db :db} [_ loc file-type results]]
   (set-download-link-properties results (:file-type file-type))
   {:db db}))

(reg-event-fx
 :exporttable/run-export-query
 ;; default query dispatch to download *all* the rows for this query
 ;; we can't use the data we have because it's limited to the first page or two
 ;; around 40 records. Many queries are bigger than this.
 (sandbox)
 (fn [{db :db} [_ loc file-type]]
   {:im-tables/im-operation
    {:on-success [:exporttable/download-export-response loc file-type]
     :op         (partial fetch/fetch-custom-format
                          (get db :service)
                          (get db :query)
                          {:format (:file-type file-type)})}
    :db db}))

(reg-event-fx
 :exporttable/run-fasta-query
 ;;like run-export-query but limited to selecting one column as required by
 ;;the fasta endpoint
 (sandbox)
 (fn [{db :db} [_ loc file-type]]
   (let [query (get db :query)
         fasta-query (assoc query :select ["id"])]
     {:im-tables/im-operation
      {:on-success [:exporttable/download-export-response loc file-type]
       :op         (partial fetch/fasta
                            (get db :service)
                            fasta-query)}
      :db db})))
