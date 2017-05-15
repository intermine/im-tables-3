(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join]]
            [imcljs.fetch :as fetch]
            [im-tables.interceptors :refer [sandbox]]))

;; REMEMBER KIDS, some gene identifiers have a comma in them, because insanity.
;; This means we default to tsv for Good Reasons.


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

(defn generate-fasta [_ query];;the first arg is query-results. We don't care.
  ()
  )

;;config for various file types
(def xsv {:csv {:file-type "csv" :action (partial stringify-query-results ",") }
          :tsv {:file-type "tsv" :action (partial stringify-query-results "\t")}
          :fasta {:file-type "fasta" :action :s}
          })


(reg-event-db
 :exporttable/set-format
 (sandbox)
 (fn [db [_ loc format]]
   ;;sets preferred format for the file export
   (assoc-in db [:settings :data-out :format] (keyword format))))

(defn generate-file
  "Selects the appropriate file type to generated based upon the :action parameter in xsv"
  [query-results file-type query]
  (let [generate-file-function (:action file-type)
        generated-file (generate-file-function query-results query)]
  (encode-file generated-file (:file-type file-type))))

(reg-event-fx
 :exporttable/download
 (sandbox)
 (fn [{db :db}]
   (let [query-results (get-in db [:query-response :results])
         file-type ((get-in db [:settings :data-out :format]) xsv)
         query (get-in db [:query])]
     ;(ocall js/window "open" (generate-file query-results file-type query))
     {:db db
      :dispatch [:exporttable/run-fasta-query]})))

;
(reg-event-fx
  :exporttable/download-fasta-response
  (sandbox)
  (fn [{db :db} [_ loc results]]
    (ocall js/window "open" (encode-file results "fasta"))
    {:db db}))

(defn select-column-for-fasta [query]
  ;;;;;
  ;;TODO: given a query, get rid of all of the columns except one that's on the good list of
  ;; fasta-columns. then query that.
  ;; once that's done, re-enable tsv csv export and refactor to sanity.
  ;;;;;
  )

(reg-event-fx
  :exporttable/run-fasta-query
  (sandbox)
  (fn [{db :db} [_ loc]]
    (.log js/console "Running FASTA query" (get db :query))
    (let [query (get db :query)
          fasta-query (assoc query :select ["id"])]
      {:im-tables/im-operation {:on-success [:exporttable/download-fasta-response loc]
                                :op         (partial fetch/fasta
                                                     (get db :service)
                                                     fasta-query)}
       :db db})))
