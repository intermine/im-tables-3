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
  (ocall js/window "encodeURI" (str "data:text/" filetype ";charset=utf-8," data)))

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
   (assoc-in db [:settings :data-out :format] (keyword format))))

(reg-event-fx
 :exporttable/download
 ;;the main action to download files. This gets called by the download modal button.
 (sandbox)
 (fn [{db :db}]
   (let [query-results (get-in db [:query-response :results])
         file-type ((get-in db [:settings :data-out :format]) xsv)
         query (get-in db [:query])]
     (if (= (:file-type file-type) "fasta")
       {:db db :dispatch [:exporttable/run-fasta-query]}
       {:db db :exporttable/download-simple-text [query-results file-type query]}))))

(defn set-download-link-properties
  "We're setting the attributes of a link with the download property enabled.
   This spawns a nice download without having to create a new window
   which might be pop-up blocked. It also allows us to set the filename.
   We live in the future!"
  [results file-type]
  (let [downloadlink (ocall js/document :getElementById "hiddendownloadlink")]
    (ocall downloadlink :setAttribute "href" (encode-file results file-type))
    (ocall downloadlink :setAttribute "download" (str "results." file-type))
    (ocall downloadlink :click)
))

(reg-event-fx
 :exporttable/download-fasta-response
 ;;generating a FASTA download requires an additional server call.
 ;;this event is dispatched after the fasta query has been downloaded successfully
 (sandbox)
 (fn [{db :db} [_ loc results]]
   (set-download-link-properties results "fasta")
   {:db db}))

(reg-fx
  :exporttable/download-simple-text
  ;;csv/tsv simple text downloads
  (fn [[query-results file-type query]]
    (let [generated-file (stringify-query-results file-type query-results)]
      (set-download-link-properties generated-file (:file-type file-type)))))

(reg-event-fx
 :exporttable/run-fasta-query
  ;;generating a FASTA file requires another server call - unlike csv/tsv it
 ;;isn't generated client side
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [query (get db :query)
         fasta-query (assoc query :select ["id"])]
     {:im-tables/im-operation {:on-success [:exporttable/download-fasta-response loc]
                               :op         (partial fetch/fasta
                                                    (get db :service)
                                                    fasta-query)}
      :db db})))
