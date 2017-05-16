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
  [separator query-results _] ;we don't care about the query arg
  (let [vec-file
        (reduce (fn [new-str [i rowvals]]
                  (conj new-str
                        (join separator (reduce (fn [new-sub-str rowval]
                                                  (conj new-sub-str (:value rowval))) [] rowvals)))) [] query-results)]
    (join "\n" vec-file)))

;;config for various file types
(def xsv {:csv {:file-type "csv" :action (partial stringify-query-results ",")}
          :tsv {:file-type "tsv" :action (partial stringify-query-results "\t")}
          :fasta {:file-type "fasta" :action nil}})

(reg-event-db
 :exporttable/set-format
 (sandbox)
 (fn [db [_ loc format]]
   ;;sets preferred format for the file export
   (assoc-in db [:settings :data-out :format] (keyword format))))

(defn generate-file
  "Selects the appropriate file type to generated based upon the :action parameter in xsv"
  [query-results file-type query]
  (let [generate-file-function (:action file-type)]
        (generate-file-function query-results query)
  ))

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

(defn set-download-link-properties [results file-type]
  (.log js/console "%cresults" "background:DEEPSKYBLUE; border-radius:2px;" (clj->js results) (.log js/console "%cfile-type" "background:DEEPSKYBLUE; border-radius:2px;" (clj->js file-type)))
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
  ;;csv/tsv simple text downloats
  (fn [[query-results file-type query]]
    (set-download-link-properties (generate-file query-results file-type query) (:file-type file-type))))


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
