(ns im-tables.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx dispatch]]
    ;[day8.re-frame.undo :as undo :refer [undoable]]
            [joshkh.undo :as undo :refer [undoable]]
    ;[joshkh.re-frame.undo :as undo :refer [undoable]]
            [im-tables.db :as db]
            [im-tables.effects]
            [im-tables.interceptors :refer [sandbox]]
            [im-tables.events.boot]
            [im-tables.events.pagination]
            [im-tables.events.exporttable]
            [imcljs.save :as save]
            [imcljs.fetch :as fetch]
            [imcljs.path :as im-path]
            [imcljs.query :as query]
            [imcljs.internal.utils :refer [scrub-url]]
            [oops.core :refer [oapply ocall oget]]
            [clojure.string :as string :refer [split join starts-with? trim]]
            [clojure.set :as set]
            [cljs.core.async :refer [close! <! chan]]
            [reagent.core :as r]
            [im-tables.utils :refer [response->error constraints->logic]]))

(joshkh.undo/undo-config!
  ; This function is used to only store certain parts
  ; of our app-db. We're specifically ignoring anything
  ; in the :cache or :response (query results) keys
 {:harvest-fn (fn [ratom location]
                (-> @ratom
                     ; The ratom contains all undos for all tables;
                     ; so only be sure to only look in THIS table's location
                    (get-in location)
                     ; These keys' values will be stored in the undo stack for this table's location.
                     ; Query is kept because duh. Keeping :settings means restoring our pagination
                     ; and temp-query is used by various controls as a staging area before merging the
                     ; changes into the :query key and running it
                    (select-keys [:query :settings :temp-query])))
   ; This function is used to put some old state back into our app-db
  :reinstate-fn (fn [ratom value location]
                   ; Ratom is our app-db
                  (swap! ratom update-in location merge value))
   ; This function fires after EITHER an :undo or a :redo takes place
   ; We're not storing any "side effect" data, such as query results because we have no control over them.
   ; Most undo events modify the underlying query so it makes sense re-run it when the query has been
   ; reverted or restored.

   ; If we ever need some undos to *not* re-run a query then consider modifying the :undo effect
   ; to either accept an optional function to run (or at least a flag to run / not run this one.
   ; I didn't do this because I'm lazy.
  :post-reinstate-fn (fn [location]
                       (dispatch [:im-tables.main/run-query location]))})

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
   ;; This event only exists so it can be intercepted by BlueGenes to display
   ;; an alert. So do not delete it even if it looks like a noop!
   db))

(reg-event-db
 :imt.io/save-list-failure
 (fn [db [_ response]]
   ;; This event only exists so it can be intercepted by BlueGenes to display
   ;; an alert. So do not delete it even if it looks like a noop!
   db))

(reg-event-fx
 :imt.io/save-list
 (sandbox)
 (fn [{db :db} [_ loc name query options]]
   {:db db
    :im-tables/im-operation {:on-success [:imt.io/save-list-success]
                             :on-failure [:imt.io/save-list-failure]
                             :op (partial save/im-list-from-query (get db :service) name (dissoc query :sortOrder :joins) options)}}))

(reg-event-db
 :modal/open
 (sandbox)
 (fn [db [_ loc contents]]
   (assoc-in db [:cache :modal] contents)))

(reg-event-db
 :modal/close
 (sandbox)
 (fn [db [_ loc]]
   (assoc-in db [:cache :modal] nil)))

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

(defn consts->letter
  "Takes a sequence of constraints and returns the first unused letter.
  Expects all applicable constraints to have a `:code` key so make sure the
  query has been run through `imcljs.query/sterilize-query`."
  [consts]
  (->> consts
       (keep :code)
       (first-letter)))

(defn logic-append
  "Appends a new letter to a possibly empty constraint logic string."
  [constlogic letter]
  (if (empty? constlogic)
    letter
    (str constlogic " and " letter)))

(defn constraint-append
  "Append a new constraint to a query map. Computes the next letter to be assoced
  as `:code` on the constraint, and appends it to the `:constraintLogic` string."
  [query constraint]
  (if (contains? constraint :type) ; Type constraints don't have a code.
    (update query :where (fnil conj []) constraint)
    (let [letter (consts->letter (:where query))
          constraint+code (assoc constraint :code letter)]
      (-> query
          (update :where (fnil conj []) constraint+code)
          (update :constraintLogic logic-append letter)))))

(reg-event-db
 :main/set-temp-query
 (sandbox)
 (fn [db [_ loc]]
   (assoc db :temp-query (get db :query))))

(reg-event-fx
 :filters/update-constraint
 (sandbox)
 (fn [{db :db} [_ loc index new-constraint]]
   (let [constraints (get-in db [:temp-query :where])
         const-logic (constraints->logic constraints)]
     {:db (-> db
              (assoc-in [:temp-query :where index] new-constraint)
              (assoc-in [:temp-query :constraintLogic] const-logic))})))

(reg-event-fx
 :filters/add-constraint
 (sandbox)
 (fn [{db :db} [_ loc new-constraint]]
   {:db (update db :temp-query constraint-append new-constraint)}))

(reg-event-fx
 :filters/remove-constraint
 (sandbox)
 (fn [{db :db} [_ loc const]]
   (let [constraints  (get-in db [:temp-query :where])
         constraints' (filterv #(not= const %) constraints)
         const-logic  (constraints->logic constraints')]
     {:db (-> db
              (assoc-in [:temp-query :where] constraints')
              (assoc-in [:temp-query :constraintLogic] const-logic))})))

(reg-event-fx
 :filters/save-changes
 [(sandbox) (undoable)]
 (fn [{db :db} [_ loc]]
   (let [db (update-in db [:temp-query :constraintLogic]
                       ;; Recompute the constraintLogic if it's empty.
                       ;; (Can only occur if the user was evil and deleted it.)
                       (comp #(or % (constraints->logic (get-in db [:temp-query :where])))
                             not-empty trim))
         added (not-empty (set/difference (set (:where (:temp-query db)))
                                          (set (:where (:query db)))))
         removed (not-empty (set/difference (set (:where (:query db)))
                                            (set (:where (:temp-query db)))))
         changed-logic (not= (get-in db [:temp-query :constraintLogic])
                             (get-in db [:query :constraintLogic]))
         model (assoc (get-in db [:service :model])
                      :type-constraints (get-in db [:temp-query :where]))]
      ; This event is usually fired when the filter dropdown closed which means it's fired
      ; a lot even when not necessary. To prevent multiple blank undos from piling up, we only
      ; attach the :undo side effect when something was actually added or removed from the query
     (cond-> {:db (assoc db :query (get db :temp-query))}
       (or added removed changed-logic)
       (assoc
        :undo {:message [:div
                         (when added
                           [:div
                            [:div "Added filters"]
                            (into [:span]
                                  (map (fn [{:keys [path op value]}]
                                         [:div.table-history-detail
                                          [:div (str (im-path/friendly model path))]
                                          [:div (str op " " value)]])
                                       added))])
                         (when removed
                           [:div
                            [:div "Removed filters"]
                            (into [:span]
                                  (map (fn [{:keys [path op value]}]
                                         [:div.table-history-detail
                                          [:div (str (im-path/friendly model path))]
                                          [:div (str op " " value)]])
                                       removed))])
                         (when changed-logic
                           [:div
                            [:div "Changed logic"]
                            [:span
                             [:div.table-history-detail
                              [:div (get-in db [:query :constraintLogic])]]
                             [:div "to"]
                             [:div.table-history-detail
                              [:div (get-in db [:temp-query :constraintLogic])]]]])]
               :location loc}
        :dispatch [:im-tables.main/run-query loc])))))

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
 :tree-view/clear-state
 (sandbox)
 (fn [db [_ loc path-vec]]
   (update-in db [:cache] dissoc :tree-view)))

(reg-event-db
 :tree-view/toggle-selection
 (sandbox)
 (fn [db [_ loc path-vec]]
   (update-in db [:cache :tree-view :selection] toggle-into-set path-vec)))

(reg-event-fx
 :tree-view/merge-new-columns
 [(sandbox) (undoable)]
 (fn [{db :db} [_ loc]]
   (let [columns-to-add (map (partial clojure.string/join ".") (get-in db [:cache :tree-view :selection]))
         model (assoc (get-in db [:service :model])
                      :type-constraints (get-in db [:query :where]))]
     {:db (-> db
              (update-in [:query :select] #(apply conj % columns-to-add))
              (assoc-in [:cache :tree-view :selection] #{}))
      :dispatch [:im-tables.main/run-query loc]
      :undo {:message [:div
                       (str "Added " (count columns-to-add) " new column" (when (> (count columns-to-add) 1) "s"))
                       (into [:div.table-history-detail] (map (fn [s] [:div (im-path/friendly model s)]) columns-to-add))]
             :location loc}})))

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
 [(sandbox) (undoable)]
 (fn [{db :db} [_ loc]]
   (let [pre-joins (set (get-in db [:query :joins]))
         post-joins (set (get-in db [:cache :rel-manager :joins]))
         added (not-empty (set/difference post-joins pre-joins))
         removed (not-empty (set/difference pre-joins post-joins))
         model (assoc (get-in db [:service :model])
                      :type-constraints (get-in db [:query :where]))]
     {:db (assoc db :query (get-in db [:cache :rel-manager]))
      :dispatch [:im-tables.main/run-query loc]
      :undo {:location loc
             :message [:div
                       (when added
                         [:div
                          [:div "Columns made mandatory"]
                          (into [:div.table-history-detail]
                                (map (fn [c] [:div (im-path/friendly model c)]) added))])
                       (when removed
                         [:div
                          [:div "Columns made optional"]
                          (into [:div.table-history-detail]
                                (map (fn [c] [:div (im-path/friendly model c)]) removed))])]}})))

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
       (not= dragged-item dragged-over)
       (assoc :dispatch [:im-tables.main/run-query loc])))))

;;;;; MANIPULATE QUERY

(reg-event-db
 :main/store-possible-values
 (sandbox)
 (fn [db [_ loc view res]]
   (assoc-in db [:cache :possible-values view]
             (if (map? res)
               (->> res :results (map :item) (remove nil?) sort)
               res)))) ; false when too many results and empty list when no possible values.

(reg-event-fx
 :main/fetch-possible-values
 (sandbox)
 (fn [{db :db} [_ loc view temp-query?]]
   (let [query (get db (if temp-query? :temp-query :query))
         model (assoc (get-in db [:service :model]) :type-constraints (:where query))]
     (cond-> {:db db}
       (not (im-path/class? model view))
       (assoc :im-tables/im-operation {:on-success [:main/store-possible-values loc view]
                                       :op (partial fetch/unique-values
                                                    (get db :service)
                                                    (assoc query :select [view])
                                                    view
                                                    1000)})))))

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
                             :op (partial fetch/rows
                                          (get db :service)
                                          (get db :query)
                                          {:summaryPath view
                                           :size 1000
                                           :format "jsonrows"})}}))

(reg-event-fx
 :main/apply-summary-filter
 [(sandbox) (undoable)]
 (fn [{db :db} [_ loc view]]
   (let [model (assoc (get-in db [:service :model])
                      :type-constraints (get-in db [:query :where]))]
     (if-let [current-selection (keys (get-in db [:cache :column-summary view :selections]))]
       {:db (update db :query constraint-append {:path view
                                                 :op "ONE OF"
                                                 :values current-selection})
        :dispatch [:im-tables.main/run-query loc]
        :undo {:location loc
               :message [:div
                         [:div [:div
                                (str (im-path/friendly model view))
                                [:div "Must be one of:"]]]
                         (into [:div.table-history-detail]
                               (map (fn [v]
                                      [:div v]) current-selection))]}}
       {:db db}))))

(reg-event-fx
 :main/apply-numerical-filter
 [(sandbox) (undoable)]
 (fn [{db :db} [_ loc view {:keys [from to]}]]
   (let [model (assoc (get-in db [:service :model])
                      :type-constraints (get-in db [:query :where]))]
     (let [existing-constraints (get-in db [:query :where])
           existing-from? (not-empty (filter (comp (partial = [view ">="]) (juxt :path :op)) existing-constraints))
           existing-to? (not-empty (filter (comp (partial = [view "<="]) (juxt :path :op)) existing-constraints))]
       {:db (update db :query
                    #(cond-> %
                       (and from existing-from?)
                       (update :where
                               (partial mapv
                                        (fn [{:keys [path op] :as c}]
                                          (if (and (= path view) (= op ">="))
                                            (assoc c :value from)
                                            c))))

                       (and from (not existing-from?))
                       (constraint-append {:path view
                                           :op ">="
                                           :value from})

                       (and to existing-to?)
                       (update :where
                               (partial mapv
                                        (fn [{:keys [path op] :as c}]
                                          (if (and (= path view) (= op "<="))
                                            (assoc c :value to)
                                            c))))

                       (and to (not existing-to?))
                       (constraint-append {:path view
                                           :op "<="
                                           :value to})))
        :dispatch [:im-tables.main/run-query loc]
        :undo {:location loc
               :message [:div
                         (str (im-path/friendly model view))
                         (cond-> [:div "Must be:"]
                           from (conj [:div.table-history-detail ">= " from])
                           to (conj [:div.table-history-detail "<= " to]))]}}))))

(reg-event-fx
 :main/remove-view
 [(sandbox) (undoable)]
 (fn [{db :db} [_ loc view]]
   (let [path-is-join? (some? (some #{view} (get-in db [:query :joins])))
         model (assoc (get-in db [:service :model])
                      :type-constraints (get-in db [:query :where]))]
     {:db (cond-> db
            (not path-is-join?) (update-in [:query :select] (partial remove (fn [v] (= v view))))
            path-is-join? (update-in [:query :select] (partial remove (fn [v] (starts-with? v view))))
            path-is-join? (update-in [:query :joins] (partial remove (fn [v] (= v view)))))
      :dispatch [:im-tables.main/run-query loc]
      :undo {:message [:div (str "Removed column")
                       [:div.table-history-detail
                        [:span (im-path/friendly model view)]]]
             :location loc}})))

(reg-event-fx
 :main/sort-by
 (sandbox)
 (fn [{db :db} [_ loc view]]
   (let [view (join "." (drop 1 (split view ".")))
         [current-sort-by] (get-in db [:query :sortOrder])
         update? (= view (:path current-sort-by))
         current-direction (get-in db [:query :sortOrder 0 :direction])]
     {:db (if update?
            ;; Toggle direction between ascending and descending if it's present.
            (update-in db [:query :sortOrder 0]
                       assoc :direction (case current-direction
                                          "ASC" "DESC"
                                          "DESC" "ASC"))
            ;; Else add an ascending sortOrder.
            (assoc-in db [:query :sortOrder]
                      [{:path view
                        :direction "ASC"}]))
      :dispatch [:im-tables.main/run-query loc]})))


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
     (assoc :im-tables/im-operation
            {:on-success
             [:main/cache-item-summary loc]
             :op
             (partial fetch/records
                      (get db :service)
                      (summary-query
                       (assoc item :summary-fields
                              (into []  (get-in db [:service :summary-fields (keyword class)])))))}))))

;;;;;;;;;;;;;;


(defn index-map
  [results offset]
  (->> results
       (map-indexed (fn [idx item] [(+ idx offset) item]))
       (into {})))

(reg-event-db
 :main/replace-query-response
 (sandbox)
 (fn [db [_ loc {:keys [start]} response]]
   (assoc db :response (update response :results index-map start))))

(reg-event-db
 :main/merge-query-response
 (sandbox)
 (fn [db [_ loc {:keys [start]} response]]
   (let [old-results (get-in db [:response :results])
         new-results (index-map (:results response) start)]
     (assoc db :response (assoc response :results (merge old-results new-results))))))

(reg-event-fx
 :im-tables.main/run-query
 (sandbox)
 (let [previous-requests (atom {})]
   (fn [{db :db} [_ loc merge?]]
     (let [{:keys [start limit] :as pagination} (get-in db [:settings :pagination])]
        ;(js/console.log "Running query: " loc (get db :query))

        ; Previous requests are stored in an atom containing a map. This is to prevent
        ; one table from cancelling a pending request belonging to another table.

        ; Close a previous request if it exists for this table's "location"
       (some-> @previous-requests (get loc) close!)
        ; Make a new request and replace the old request with the new one
       (swap! previous-requests assoc loc (fetch/table-rows
                                           (get db :service)
                                           (get db :query)
                                           {:start start
                                            :size (* limit (get-in db [:settings :buffer]))}))
       {:db (-> db
                (assoc-in [:cache :column-summary] {})
                (dissoc :error))
        :dispatch [:main/deconstruct loc :force? true]
        :im-tables/im-operation-chan
        {; Hand the request atom off to the effect that takes from it
         :channel (get @previous-requests loc)
         :on-success (if merge?
                       ^:flush-dom [:main/merge-query-response loc pagination]
                       ^:flush-dom [:main/replace-query-response loc pagination])
         :on-failure ^:flush-dom [:error/response loc]}}))))

(reg-event-db
 :main/save-decon-count
 (sandbox)
 (fn [db [_ loc path count]]
   (assoc-in db [:query-parts-counts path] (js/parseInt count 10))))

(reg-event-fx
 :main/count-deconstruction
 (sandbox)
 (fn [{db :db} [_ loc path details]]
   {:db db
    :im-tables/im-operation {:on-success [:main/save-decon-count loc path]
                             :op (partial fetch/row-count
                                          (get db :service)
                                          (dissoc (get details :query)
                                                  ;; These have no effect on the count, and might cause an error if InterMine determines them to be irrelevant.
                                                  :joins :sortOrder :orderBy))}}))

(reg-event-fx
 :main/deconstruct
 (sandbox)
 (fn [{db :db} [_ loc & {:keys [force?]}]]
   (let [deconstructed-query (->> (query/deconstruct-by-class
                                   (assoc (get-in db [:service :model])
                                          :type-constraints (get-in db [:query :where]))
                                   (get-in db [:query]))
                                  vals
                                  (apply merge))]
     (cond-> {:db (assoc db :query-parts deconstructed-query)}
       force? (assoc :dispatch-n
                     (into []
                           (map (fn [[part details]]
                                  [:main/count-deconstruction loc part details])
                                deconstructed-query)))))))

(reg-event-fx
 :main/set-codegen-option
 (sandbox)
 (fn [{db :db} [_ loc option value run?]]
   (let [gen-xml? (= [option value] [:lang "xml"])]
     (cond-> {:db (assoc-in db [:settings :codegen option] value)}
       (and run? (not gen-xml?)) (assoc :dispatch [:main/generate-code loc])
       gen-xml? (update-in [:db :codegen] assoc
                           :code (query/->xml (assoc (get-in db [:service :model])
                                                     :type-constraints (get-in db [:query :where]))
                                              (get-in db [:query]))
                           :lang "xml")))))


(reg-event-fx
 :main/generate-code
 (sandbox)
 (let [fetch-atom (r/atom (chan))]
   (fn [{db :db} [_ loc]]
      ; Close any previous code generation requests
     (swap! fetch-atom close!)
     (let [{:keys [query service]} db
           lang (get-in db [:settings :codegen :lang])
           new-request (fetch/code service (:model service) {:query query :lang lang})]
        ; Store the new request
       (reset! fetch-atom new-request)
        ; Clear the old code
       {:db (-> db (assoc-in [:codegen :code] nil))
        :im-tables/im-operation-chan
        {:on-success [:main/save-codegen loc lang]
         :channel new-request}}))))

(reg-event-db
 :main/save-codegen
 (sandbox)
 (fn [db [_ loc lang response]]
   (update db :codegen assoc :code response :lang lang)))

(reg-event-db
 :filter-manager/change-constraint
 (sandbox)
 (fn [db [_ loc value]]
   (assoc-in db [:temp-query :constraintLogic] value)))

(reg-event-fx
 :main/retry-failure
 (sandbox)
 (fn [{db :db} [_ loc]]
   {:db (dissoc db :error)
    :dispatch [:im-tables/reload loc]}))

(reg-event-fx
 :error/response
 (sandbox)
 (fn [{db :db} [_ loc res]]
   (let [{error-type :type :as error-map} (response->error res)]
     (cond-> {:db (-> db
                      (assoc :error error-map)
                      ;; We also want to remove any previous successful response.
                      (dissoc :response))}
       (= error-type :network)
       (assoc :im-tables/log-error ["Network error" {:response res}])))))
