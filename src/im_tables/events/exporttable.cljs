(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join]]
            [im-tables.interceptors :refer [sandbox]]))

;; REMEMBER KIDS, some gene identifiers have a comma in them, because insanity.
;; This means we default to tsv for Good Reasons.

(def xsv {:csv {:file-type "csv" :separator ","}
          :tsv {:file-type "tsv" :separator "\t"}})

(defn encode-file
  "Encode a stringified text file such that it can be downloaded by the browser.
  Results must be stringified - don't pass objects / vectors / arrays / whatever."
  [data filetype]
  (ocall js/window "encodeURI" (str "data:text/" filetype ";charset=utf-8," data)))

(defn stringify-query-results
  "converts results into a csv/tsv-style string."
  [query-results separator]
  (reduce (fn [new-str [i rowvals]]
            (str  new-str
                  (reduce (fn [new-sub-str rowval]
                            (str new-sub-str separator " " (:value rowval))) "" rowvals)
                  "\n")) "" query-results))

(reg-event-fx
 :exporttable/exporttable
 (sandbox)
 (fn [{db :db}]
   (let [query-results (get-in db [:query-response :results])
         file-type (:tsv xsv)
         stringified-file (stringify-query-results query-results (:separator file-type))]
     (ocall js/window "open" (encode-file stringified-file (:file-type file-type)))
     {:db db})))
