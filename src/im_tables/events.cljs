(ns im-tables.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx]]
            [day8.re-frame.async-flow-fx]
            [day8.re-frame.undo :as undo :refer [undoable]]
            [im-tables.db :as db]
            [im-tables.effects]
            [im-tables.interceptors :refer [sandbox]]
            [im-tables.events.boot]
            [im-tables.events.pagination]
            [im-tables.events.exporttable]
            [imcljs.save :as save]
            [imcljs.fetch :as fetch]
            [imcljs.query :as query]
            [oops.core :refer [oapply ocall oget]]
            [clojure.string :refer [split join starts-with?]]))

(reg-event-db
  :printdb
  (fn [db]
    (.log js/console "DB" db)
    db))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(reg-event-fx
  :im-tables.main/replace-all-state
  (sandbox)
  (fn [_ [_ loc state]]
    {:db (deep-merge db/default-db state)
     :dispatch [:im-tables.main/run-query loc]}))



(reg-event-db
  :imt.io/save-list-success
  (fn [db [_ response]]
    (.debug js/console "List Saved" response)
    db))


(reg-event-fx
  :imt.io/save-list
  (sandbox)
  (fn [{db :db} [_ loc name query options]]
    {:db db
     :im-tables/im-operation {:on-success [:imt.io/save-list-success]
                              :op (partial save/im-list (get db :service) name query options)}}))

(reg-event-fx
  :prep-modal
  (sandbox)
  (fn [{db :db} [_ loc contents]]
    {:db (assoc-in db [:cache :modal] contents)}))

(reg-event-fx
  :modal/close
  (sandbox)
  (fn [{db :db} [_ loc]]
    (let [modal (ocall js/document :getElementById "testModal")]
      ;;feigning a click is easier than dismissing it programatically for some reason
    (ocall modal "click"))
    {:db (assoc-in db [:cache :modal] nil)}))

(reg-event-db
  :show-overlay
  (sandbox)
  (fn [db [_ loc]]
    (assoc-in db [:cache :overlay?] true)))

(reg-event-db
  :hide-overlay
  (sandbox)
  (fn [db [_ loc]]
    (assoc-in db [:cache :overlay?] false)))



(defn toggle-into-set [haystack needle]
  (if (some #{needle} haystack)
    (filter (fn [n] (not= n needle)) haystack)
    (conj haystack needle)))

(defn flip-presence
  "If a key is present in a map then remove it, otherwise add the key with a value of true."
  [m k]
  (if (contains? m k) (dissoc m k) (assoc m k true)))


;;;; FILTERS

(def alphabet (clojure.string/split "ABCDEFGHIJKLMNOPQRSTUVWXYZ" ""))

(defn haystack-has? [haystack needle]
  (some? (some #{needle} haystack)))

(defn first-letter [letters]
  (first (drop-while (partial haystack-has? letters) alphabet)))

(reg-event-db
  :main/set-temp-query
  (sandbox)
  (fn [db [_ loc]]
    (assoc db :temp-query (get db :query))))

(reg-event-db
  :filters/update-constraint
  (sandbox)
  (fn [db [_ loc new-constraint]]
    (update-in db [:temp-query :where]
               (fn [constraints]
                 (map (fn [constraint]
                        (if (= (:code new-constraint) (:code constraint))
                          new-constraint
                          constraint)) constraints)))))



(reg-event-fx
  :filters/add-constraint
  (sandbox)
  (fn [{db :db} [_ loc new-constraint]]
    (.log js/console "ADDING CONSTRAINT" new-constraint)
    {:dispatch [:filters/save-changes loc]
     :db (update-in db [:temp-query :where]
                    (fn [constraints]
                      (conj constraints (assoc new-constraint :code (first-letter (map :code constraints))))))}))


(reg-event-fx
  :filters/remove-constraint
  (sandbox)
  (fn [{db :db} [_ loc new-constraint]]
    (.log js/console "removing constraint" loc new-constraint)
    {:dispatch [:filters/save-changes loc]
     :db (update-in db [:temp-query :where]
                    (fn [constraints]
                      (remove nil? (map (fn [constraint]
                                          (if (= constraint new-constraint)
                                            nil
                                            constraint)) constraints))))}))

(reg-event-fx
  :filters/save-changes
  (sandbox)
  (fn [{db :db} [_ loc]]
    {:db (assoc db :query (get db :temp-query))
     :dispatch [:im-tables.main/run-query loc]}))

;;;; TRANSIENT VALUES

;TODO turn stub into working code
(reg-event-db
  :select/toggle-selection
  (sandbox)
  (fn [db [_ loc view value]]
    (update-in db [:cache :column-summary view :selections] flip-presence value)))

(reg-event-db
  :select/clear-selection
  (sandbox)
  (fn [db [_ loc view]]
    (assoc-in db [:cache :column-summary view :selections] {})))

(reg-event-db
  :select/select-all
  (sandbox)
  (fn [db [_ loc view]]
    (assoc-in db [:cache :column-summary view :selections]
              (into {} (map (fn [{item :item}] [item true]) (get-in db [:cache :column-summary view :response :results]))))))

(reg-event-db
  :select/set-text-filter
  (sandbox)
  (fn [db [_ loc view value]]
    (assoc-in db [:cache :column-summary view :filters :text]
              (if (= value "") nil value))))

;;;;; TREE VIEW
(reg-event-db
  :tree-view/toggle-selection
  (sandbox)
  (fn [db [_ loc path-vec]]
    (update-in db [:cache :tree-view :selection] toggle-into-set path-vec)))

(reg-event-fx
  :tree-view/merge-new-columns
  (sandbox)
  (fn [{db :db} [_ loc]]
    (let [columns-to-add (map (partial clojure.string/join ".") (get-in db [:cache :tree-view :selection]))]
      {:db (-> db
               (update-in [:query :select] #(apply conj % columns-to-add))
               (assoc-in [:cache :tree-view :selection] #{}))
       :dispatch [:im-tables.main/run-query loc]
       })))


(defn coll-contains? [needle haystack] (some? (some #{needle} haystack)))
(defn without [coll item] (filter (partial not= item) coll))


;;;;; Relationship Manager

; Copy the main query to our cache for editing
(reg-event-db
  :rel-manager/reset
  (sandbox)
  (fn [db [_ loc]]
    (assoc-in db [:cache :rel-manager] (get db :query))))

; Copy the edited query from the cache back to the main query and run it
(reg-event-fx
  :rel-manager/apply-changes
  (sandbox)
  (fn [{db :db} [_ loc]]
    {:db (assoc db :query (get-in db [:cache :rel-manager]))
     :dispatch [:im-tables.main/run-query loc]}))

(reg-event-db
  :rel-manager/toggle-relationship
  (sandbox)
  (fn [db [_ loc view join?]]
    (if join?
      (update-in db [:cache :rel-manager :joins] conj view)
      (update-in db [:cache :rel-manager :joins] without view))))


;;;;; STYLE

(defn swap
  "Given a collection, swap the positions of elements at index-a and index-b with eachother"
  [coll index-a index-b]
  (assoc coll index-b (coll index-a) index-a (coll index-b)))

(defn drop-nth
  "Drop a value from a collection at a specific index"
  [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))

(defn insert-nth
  "Insert a value into a collection at a specifix"
  [coll n item]
  (apply concat ((juxt first (comp (partial cons item) second)) (split-at n coll))))

(defn move-nth
  "Shift an item from one index in a collection to another "
  [coll from-idx to-idx]
  (insert-nth (drop-nth from-idx coll) to-idx (nth coll from-idx)))

(defn index
  "Given a predicate and a collection, find the index of the first truthy value
  (index (partial = :b) [:a :b :c]) => 1
  (index #(clojure.string/sarts-with? % xyz) [abc1 def2 xyz3]) => 2"
  [pred coll]
  (first (filter some? (map-indexed (fn [idx n] (when (pred n) idx)) coll))))

(defn begins-with? "Clojure.string/starts-with? with reversed argument order"
  [substring string]
  (clojure.string/starts-with? string substring))

(reg-event-db
  :style/dragging-item
  (sandbox)
  (fn [db [_ loc view]]
    (let [outer-join? (some? (some #{view} (get-in db [:query :joins])))]
      (if outer-join?
        ; If the column (view) being dragged is part of an outer join then get the idx of the first occurance
        (assoc-in db [:cache :dragging-item] (index (partial begins-with? view) (get-in db [:query :select])))
        ; Otherwise, find an identical match
        (assoc-in db [:cache :dragging-item] (index (partial = view) (get-in db [:query :select])))))))

(reg-event-db
  :style/dragging-over
  (sandbox)
  (fn [db [_ loc view]]
    (let [outer-join? (some? (some #{view} (get-in db [:query :joins])))]
      (if outer-join?
        ; If the column (view) being dragged over is part of an outer join then get the idx of the first occurance
        (assoc-in db [:cache :dragging-over] (index (partial begins-with? view) (get-in db [:query :select])))
        ; Otherwise, find an identical match
        (assoc-in db [:cache :dragging-over] (index (partial = view) (get-in db [:query :select])))))))

(reg-event-fx
  :style/dragging-finished
  (sandbox)
  (fn [{db :db} [_ loc]]
    (let [dragged-item (get-in db [:cache :dragging-item])
          dragged-over (get-in db [:cache :dragging-over])]
      (cond-> {:db (-> db
                       (update-in [:query :select] move-nth dragged-item dragged-over)
                       (update-in [:cache] dissoc :dragging-item :dragging-over))}
              (not= dragged-item dragged-over) (assoc :dispatch-n
                                                      [^:flush-dom [:show-overlay loc]
                                                       [:im-tables.main/run-query loc]])))))

;;;;; MANIPULATE QUERY

(reg-event-db
  :main/save-column-summary
  (sandbox)
  (fn [db [_ loc view summary-response]]
    (assoc-in db [:cache :column-summary view :response] summary-response)))

(reg-event-fx
  :main/summarize-column
  (sandbox)
  (fn [{db :db} [_ loc view]]
    {:db db
     :im-tables/im-operation {:on-success [:main/save-column-summary loc view]
                              :op (partial fetch/unique-values
                                           (get db :service)
                                           (get db :query)
                                           view
                                           1000)}}))

(reg-event-fx
  :main/apply-summary-filter
  ;(undoable)
  (sandbox)
  (fn [{db :db} [_ loc view]]
    (if-let [current-selection (keys (get-in db [:cache :column-summary view :selections]))]
      {:db (update-in db [:query :where] conj {:path view
                                               :op "ONE OF"
                                               :values current-selection})
       :dispatch [:im-tables.main/run-query loc]
       ;:undo     "Applying column filter"
       }
      {:db db})))

(reg-event-fx
  :main/remove-view
  (sandbox)
  ;(undoable)
  (fn [{db :db} [_ loc view]]
    (let [path-is-join? (some? (some #{view} (get-in db [:query :joins])))]
      {:db (cond-> db
                   (not path-is-join?) (update-in [:query :select] (partial remove (fn [v] (= v view))))
                   path-is-join? (update-in [:query :select] (partial remove (fn [v] (starts-with? v view))))
                   path-is-join? (update-in [:query :joins] (partial remove (fn [v] (= v view)))))
       :dispatch [:im-tables.main/run-query loc]})))


(reg-event-fx
  :main/sort-by
  (sandbox)
  (fn [{db :db} [_ loc view]]
    (let [view              (join "." (drop 1 (split view ".")))
          [current-sort-by] (get-in db [:query :orderBy])
          update?           (= view (:path current-sort-by))
          current-direction (get-in db [:query :orderBy 0 :direction])]
      {:db (if update?
             (update-in db [:query :orderBy 0]
                        assoc :direction (case current-direction
                                           "ASC" "DESC"
                                           "DESC" "ASC"))
             (assoc-in db [:query :orderBy]
                       [{:path view
                         :direction "ASC"}]))
       :dispatch [:im-tables.main/run-query loc]
       })))


;;;;;; SUMMARY CACHING

(defn summary-query [{:keys [class id summary-fields]}]
  {:from class
   :select summary-fields
   :where [{:path (str class ".id")
            :op "="
            :value id}]})

(reg-event-db
  :main/cache-item-summary
  (sandbox)
  (fn [db [_ loc response]]
    (update-in db [:cache :item-details]
               (fn [summary-map]
                 (let [{:keys [objectId] :as r} (first (:results response))]
                   (assoc summary-map objectId
                                      {:value r
                                       :views (:views response)
                                       :column-headers (:columnHeaders response)}))))))

(reg-event-fx
  :main/summarize-item
  (sandbox)
  (fn [{db :db} [_ loc {:keys [class id] :as item}]]
    (cond-> {:db db}
            (not (get-in db [:cache :item-details id]))
            (assoc :im-tables/im-operation {:on-success
                                            [:main/cache-item-summary loc]
                                            :op
                                            (partial fetch/records
                                                     (get db :service)
                                                     (summary-query
                                                       (assoc item :summary-fields
                                                                   (into [] (keys (get-in db [:service :model :classes (keyword class) :attributes]))))))}))))




;;;;;;;;;;;;;;



(reg-event-fx
  :main/replace-query-response
  (sandbox)
  (fn [{db :db} [_ loc {:keys [start size]} results]]
    (let [new-results-map (into {} (map-indexed (fn [idx item] [(+ idx start) item]) (:results results)))
          updated-results (assoc results :results new-results-map)]
      {:db (assoc db :query-response updated-results)
       ;:db         (assoc db :query-response results)
       :dispatch-n (into [^:flush-dom [:hide-overlay loc]]
                         (map (fn [view] [:main/summarize-column loc view]) (get results :views)))})))

(reg-event-fx
  :main/merge-query-response
  (sandbox)
  (fn [{db :db} [_ loc {:keys [start size]} results]]
    (let [new-results-map (into {} (map-indexed (fn [idx item] [(+ idx start) item]) (:results results)))
          updated-results (assoc results :results (merge (get-in db [:query-response :results]) new-results-map))]
      {:db (assoc db :query-response updated-results)
       ;:db         (assoc db :query-response results)
       :dispatch-n (into [^:flush-dom [:hide-overlay loc]]
                         (map (fn [view] [:main/summarize-column loc view]) (get results :views)))})))

(reg-event-fx
  :im-tables.main/run-query
  (sandbox)
  (fn [{db :db} [_ loc merge?]]
    (.debug js/console "Running query" (get db :query))
    (let [{:keys [start limit] :as pagination} (get-in db [:settings :pagination])]
      {:db (assoc-in db [:cache :column-summary] {})
       ;:undo                   "Undo ran query"
       :dispatch-n [^:flush-dom [:show-overlay loc]
                    [:main/deconstruct loc]]
       :im-tables/im-operation {:on-success (if merge?
                                              [:main/merge-query-response loc pagination]
                                              [:main/replace-query-response loc pagination])
                                :op (partial fetch/table-rows
                                             (get db :service)
                                             (get db :query)
                                             {:start start
                                              :size (* limit (get-in db [:settings :buffer]))})}})))



(reg-event-db
  :main/save-decon-count
  (sandbox)
  (fn [db [_ loc path count]]
    (assoc-in db [:query-parts path :count] count)))

(reg-event-fx
  :main/count-deconstruction
  (sandbox)
  (fn [{db :db} [_ loc path details]]
    {:db db
     :im-tables/im-operation {:on-success [:main/save-decon-count loc path]
                              :op (partial fetch/row-count
                                           (get db :service)
                                           (dissoc (get details :query) :joins))}}))

(reg-event-fx
  :main/deconstruct
  (sandbox)
  (fn [{db :db} [_ loc]]

    (let [deconstructed-query (into {} (map vec (sort-by
                                                  (fn [[p _]] (count (clojure.string/split p ".")))
                                                  (partition 2
                                                             (flatten
                                                               (map seq (vals (query/deconstruct-by-class (get-in db [:service :model]) (get-in db [:query])))))))))]


      {:db (assoc db :query-parts deconstructed-query)
       :dispatch-n (into [] (map (fn [[part details]] [:main/count-deconstruction loc part details]) deconstructed-query))})))
