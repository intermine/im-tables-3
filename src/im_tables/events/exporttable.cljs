(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join]]
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
  [query-results file-type]
  (let [separator (:separator file-type)
        vec-file
        (reduce (fn [new-str [i rowvals]]
                  (conj new-str
                        (join separator (reduce (fn [new-sub-str rowval]
                                                  (conj new-sub-str (:value rowval))) [] rowvals)))) [] query-results)]
    (join "\n" vec-file)))

    (def xsv {:csv {:file-type "csv" :separator "," :action stringify-query-results}
              :tsv {:file-type "tsv" :separator "\t" :action stringify-query-results}})


(reg-event-db
 :exporttable/set-format
 (sandbox)
 (fn [db [_ loc format]]
   ;;sets preferred format for the file export
   (assoc-in db [:settings :export :format] (keyword format))))

(defn generate-file
  "Selects the appropriate file type to generated based upon the :action parameter in xsv"
  [query-results file-type]
  (let [generate-file-function (:action file-type)
        generated-file (generate-file-function query-results (:separator file-type))]
  (encode-file generated-file (:file-type file-type))))

(reg-event-fx
 :exporttable/download
 (sandbox)
 (fn [{db :db}]
   (let [query-results (get-in db [:query-response :results])
         file-type ((get-in db [:settings :export :format]) xsv)]
     (ocall js/window "open" (generate-file query-results file-type))
     {:db db})))
