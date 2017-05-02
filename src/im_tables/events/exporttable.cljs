(ns im-tables.events.exporttable
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [oops.core :refer [oget ocall ocall! ocall+]]
            [clojure.string :refer [join]]
            [im-tables.interceptors :refer [sandbox]]))

(defn encode-file [data filetype]
  (ocall js/window "encodeURI" (str "data:text/" filetype ";charset=utf-8," data)))

(defn stringify-query-results [query-results separator]
  (reduce (fn [new-str [i rowvals]]
        (str  new-str
              (reduce (fn [new-sub-str rowval]
                        (str new-sub-str separator " " (:value rowval))) "" rowvals)
              "\n")) "" query-results))

(reg-event-fx
 :exporttable/exporttable
 (sandbox)
 (fn [{db :db} [_ _ x]]
   (let [query-results (get-in db [:query-response :results])
         stringified-file (stringify-query-results query-results ",")]
     (ocall js/window "open" (encode-file stringified-file "csv"))
     {:db db})))

;   :download true
;   :href (encode-file "kittens, dogs\n1,2" "csv")
