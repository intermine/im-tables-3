(ns im-tables.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [day8.re-frame.async-flow-fx]
            [day8.re-frame.undo :as undo :refer [undoable]]
            [im-tables.db :as db]
            [im-tables.effects]
            [imcljs.search :as search]
            [imcljs.assets :as assets]
            [oops.core :refer [oapply oget]]
            [clojure.string :refer [split join]]))

(defn boot-flow
  []
  {:first-dispatch [:main/fetch-assets]
   :rules          [{:when     :seen-all-of?
                     :events   [:main/save-summary-fields
                                :main/save-model]
                     :dispatch [:main/run-query]}]})

(re-frame/reg-event-fx
  :initialize-db
  (fn [_ _]
    {:db         db/default-db
     :async-flow (boot-flow)}))

(reg-event-fx
  :main/save-query-response
  (fn [{db :db} [_ results]]
    {:db         (assoc db :query-response results)
     :dispatch-n (into [] (map (fn [view] [:main/summarize-column view]) (get results :views)))}))


(defn toggle-into-set [haystack needle]
  (if (some #{needle} haystack)
    (filter (fn [n] (not= n needle)) haystack)
    (conj haystack needle)))

(defn flip-presence
  "If a key is present in a map then remove it, otherwise add the key
  with a value of true."
  [m k]
  (if (contains? m k)
    (dissoc m k)
    (assoc m k true)))

;;;; TRASIENT VALUES

;TODO turn stub into working code
(reg-event-db
  :select/toggle-selection
  (fn [db [_ view value]]
    (update-in db [:cache :column-summary view :selections] flip-presence value)))

(reg-event-db
  :select/clear-selection
  (fn [db [_ view]]
    (assoc-in db [:cache :column-summary view :selections] {})))

(reg-event-db
  :select/select-all
  (fn [db [_ view]]
    (assoc-in db [:cache :column-summary view :selections]
              (into {} (map (fn [{item :item}] [item true]) (get-in db [:cache :column-summary view :response :results]))))))

(reg-event-db
  :select/set-text-filter
  (fn [db [_ view value]]
    (assoc-in db [:cache :column-summary view :filters :text]
              (if (= value "") nil value))))

;;;;; TREE VIEW
(reg-event-db
  :tree-view/toggle-selection
  (fn [db [_ path-vec]]
    (update-in db [:cache :tree-view :selection] toggle-into-set path-vec)))

(reg-event-fx
  :tree-view/merge-new-columns
  (fn [{db :db} []]
    ; Drop the root of each path [Gene organism name] and create a string path "organism.name"
    (let [columns-to-add (map (comp (partial clojure.string/join ".") rest) (get-in db [:cache :tree-view :selection]))]
      {:db       (-> db
                     (update-in [:query :select] #(apply conj % columns-to-add))
                     (assoc-in [:cache :tree-view :selection] #{}))
       :dispatch [:main/run-query]})))

;;;;; MANIPULATE QUERY

(reg-event-db
  :main/save-column-summary
  (fn [db [_ summary-response]]
    ; Assume we summarized just one view
    (let [view (get-in summary-response [:views 0])]
      (assoc-in db [:cache :column-summary view :response] summary-response))))

(reg-event-fx
  :main/summarize-column
  (fn [{db :db} [_ view]]
    {:db           db
     :im-operation {:on-success [:main/save-column-summary]
                    :op         (partial search/raw-query-rows
                                         (get db :service)
                                         (assoc (get db :query) :views [view])
                                         {:summaryPath view
                                          :format      "jsonrows"})}}))

(reg-event-fx
  :main/apply-summary-filter
  ;(undoable)
  (fn [{db :db} [_ view]]
    (let [current-selection (keys (get-in db [:cache :column-summary view :selections]))]
      {:db       (update-in db [:query :where] conj {:path   view
                                                     :op     "ONE OF"
                                                     :values current-selection})
       :dispatch [:main/run-query]
       ;:undo     "Applying column filter"
       })))

(reg-event-fx
  :main/remove-view
  ;(undoable)
  (fn [{db :db} [_ view]]
    (let [view (join "." (drop 1 (split view ".")))]
      {:db       (update-in db [:query :select] (partial remove (fn [v] (= v view))))
       :dispatch [:main/run-query]
       ;:undo     "Removed column"
       })))

(reg-event-fx
  :main/sort-by
  (fn [{db :db} [_ view]]
    (let [view              (join "." (drop 1 (split view ".")))
          [current-sort-by] (get-in db [:query :orderBy])
          update?           (= view (:path current-sort-by))
          current-direction (get-in db [:query :orderBy 0 :direction])]
      {:db       (if update?
                   (update-in db [:query :orderBy 0]
                              assoc :direction (case current-direction
                                                 "ASC" "DESC"
                                                 "DESC" "ASC"))
                   (assoc-in db [:query :orderBy]
                             [{:path      view
                               :direction "ASC"}]))
       :dispatch [:main/run-query]})))


;;;;;; SUMMARY CACHING

(defn summary-query [{:keys [class id summary-fields]}]
  {:from   class
   :select summary-fields
   :where  [{:path  (str class ".id")
             :op    "="
             :value id}]})

(reg-event-db
  :main/cache-item-summary
  (fn [db [_ response]]
    (update-in db [:cache :item-details]
               (fn [summary-map]
                 (let [{:keys [objectId] :as r} (first (:results response))]
                   (assoc summary-map objectId
                                      {:value          r
                                       :views          (:views response)
                                       :column-headers (:columnHeaders response)}))))))

(reg-event-fx
  :main/summarize-item
  (fn [{db :db} [_ {:keys [class id] :as item}]]
    (cond-> {:db db}
            (not (get-in db [:cache :item-details id]))
            (assoc :im-operation {:on-success
                                  [:main/cache-item-summary]
                                  :op
                                  (partial search/raw-query-rows
                                           (get db :service)
                                           (summary-query
                                             (assoc item :summary-fields
                                                         (into [] (keys (get-in db [:assets :model (keyword class) :attributes])))))
                                           {:format "jsonobjects"})}))))

;;; PAGINATION

(reg-event-db
  :settings/update-pagination-inc
  (fn [db [_]]
    (update-in db [:settings :pagination :start] + (get-in db [:settings :pagination :limit]))))

(reg-event-db
  :settings/update-pagination-dec
  (fn [db [_]]
    (update-in db [:settings :pagination :start] - (get-in db [:settings :pagination :limit]))))

(reg-event-db
  :settings/update-pagination-fulldec
  (fn [db [_]]
    (assoc-in db [:settings :pagination :start] 0)))

(reg-event-db
  :settings/update-pagination-fullinc
  (fn [db [_]]
    (let [total (get-in db [:query-response :iTotalRecords])]
      (assoc-in db [:settings :pagination :start] (- total (mod total 10))))))

;;;;;;;;;;;;;;

(reg-event-db
  :main/save-summary-fields
  (fn [db [_ summary-fields]]
    (assoc-in db [:assets :summary-fields] summary-fields)))

(reg-event-db
  :main/save-model
  (fn [db [_ model]]
    (assoc-in db [:assets :model] model)))

(reg-event-fx
  :fetch-asset
  (fn [{db :db} [_ im-op]]
    {:db           db
     :im-operation im-op}))

(reg-event-fx
  :main/fetch-assets
  (fn [{db :db}]
    {:db         db
     :dispatch-n [[:fetch-asset {:on-success [:main/save-summary-fields]
                                 :op         (partial assets/summary-fields (get db :service))}]
                  [:fetch-asset {:on-success [:main/save-model]
                                 :op         (partial assets/model (get db :service))}]]}))

(reg-event-fx
  :main/run-query
  (undoable)
  (fn [{db :db}]
    (.debug js/console "Running query" (get db :query))
    {:db           (assoc-in db [:cache :column-summary] {})
     :undo         "Undo ran query"
     :im-operation {:on-success [:main/save-query-response]
                    :op         (partial search/table-rows
                                         (get db :service)
                                         (get db :query)
                                         {:format "json"})}}))

