(ns im-tables.events.boot
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [im-tables.db :as db]
            [im-tables.interceptors :refer [sandbox]]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [imcljs.query :as im-query]
            [im-tables.utils :refer [constraints->logic]]))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(comment
  "Think twice before you use async-flow here. Events belonging to other `loc`
  (im-tables) will be regarded as seen, so we would have to use a modified
  version which cheks the `loc` before regarding an event as seen (it's only
  a file with 200 lines, so we can copy it to this project). Anyways, because
  of this and how trivial booting an im-table is, we're not using async-flow.
  If you do decide to re-introduce it, here's an outdated example."
  (defn boot-flow
    [loc]
    {;; IDs use gensym if not provided, so there won't be any conflict anyways.
     ;; However, it's useful to identify the im-table should you need to debug.
     :id (keyword "async-flow" (str/join "-" (map name loc)))
     :first-dispatch [:imt.auth/init loc]
     :rules [{:when :seen?
              :events [:imt.auth/store-token]
              :dispatch-n [[:imt.main/fetch-model loc]
                           [:imt.main/fetch-summary-fields loc]]}
             {:when :seen-all-of?
              :events [:imt.main/save-model
                       :imt.main/save-summary-fields]
              :dispatch [:im-tables/query loc]
              :halt? true}
             {:when :seen?
              :events :error/response
              :halt? true}]}))

(reg-event-fx
 :im-tables/load
 (sandbox)
 (fn [{db :db} [_ loc {:keys [query service location response settings] :as args}]]
   (let [init-db (assoc args :settings
                        (deep-merge (:settings db/default-db) settings))
         token (get-in init-db [:service :token])
         model (get-in init-db [:service :model])
         smmry (get-in init-db [:service :summary-fields])]
     ;; We keep a copy of the initial database in `:init`, so that it can be
     ;; restored to get the fresh state of an im-table.
     (cond-> {:db (assoc init-db :init init-db)
              :dispatch-n []}
       ;; Fetch the stuff we're missing.
       (empty? token) (update :dispatch-n conj [:imt.auth/fetch-anonymous-token loc])
       (empty? model) (update :dispatch-n conj [:imt.main/fetch-model loc])
       (empty? smmry) (update :dispatch-n conj [:imt.main/fetch-summary-fields loc])
       ;; If we have everything, let's skip to running the query!
       (every? not-empty [token model smmry])
       (update :dispatch-n conj [:im-tables/query loc])))))

(defn ready-for-query?
  "Returns whether we have everything needed to run our query."
  [db]
  (let [token (get-in db [:service :token])
        model (get-in db [:service :model])
        summary-fields (get-in db [:service :summary-fields])]
    (every? not-empty [token model summary-fields])))

;; Used to reboot after an uncaught error (resets db to initial state).
;; This will throw away any changes the user has made to the original query.
(reg-event-fx
 :im-tables/restart
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [init-db (:init db)
         new-db (-> (assoc init-db :init init-db)
                    (update :service dissoc :model :summary-fields))]
     {:db new-db
      :dispatch-n [(when (empty? (get-in new-db [:service :token]))
                     [:imt.auth/fetch-anonymous-token loc])
                   [:imt.main/fetch-model loc]
                   [:imt.main/fetch-summary-fields loc]]})))

;; Used to reboot after a network error (doesn't touch db).
;; This will keep any changes the user has made to the original query.
(reg-event-fx
 :im-tables/reload
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [new-db (update db :service dissoc :model :summary-fields)]
     {:db new-db
      :dispatch-n [(when (empty? (get-in new-db [:service :token]))
                     [:imt.auth/fetch-anonymous-token loc])
                   [:imt.main/fetch-model loc]
                   [:imt.main/fetch-summary-fields loc]]})))

;; Suggestion: Re-use `:im-tables.main/run-query` instead, if suitable.
(reg-event-fx
 :im-tables/query
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [service (:service db)
         {:keys [pagination buffer]} (:settings db)
         {:keys [start limit]}       pagination
         query (im-query/sterilize-query (:query db))]
     (merge
      {:db (assoc db
                  ;; Here we can perform additional purifying to accomodate
                  ;; requirements im-tables has on the query data.
                  :query (cond-> query
                           ;; constraintLogic should always be defined.
                           (not (contains? query :constraintLogic))
                           (assoc :constraintLogic (constraints->logic (:where query)))
                           ;; constraints should be a vector
                           (list? (:where query))
                           (update :where vec)))
       :dispatch [:main/deconstruct loc]}
      (when (empty? (:response db))
        ;; Do not run query if response has been passed to im-tables in load.
        {:im-tables/im-operation-chan
         {:channel (fetch/table-rows service query {:start start
                                                    :size (* limit buffer)})
          :on-success ^:flush-dom [:main/replace-query-response loc pagination]
          :on-failure ^:flush-dom [:error/response loc]}})))))

(reg-event-db
 :initialize-db
 (sandbox)
 (fn [_] db/default-db))

;; We trust that if im-tables is passed a token,
;; it is valid (as the parent UI should handle this).
(reg-event-fx
 :imt.auth/init
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [token (get-in db [:service :token])]
     {:db db
      :dispatch (if (empty? token)
                  [:imt.auth/fetch-anonymous-token loc]
                  [:imt.auth/store-token loc token])})))

(reg-event-fx
 :imt.auth/fetch-anonymous-token
 (sandbox)
 (fn [{db :db} [_ loc]]
   {:db db
    :im-tables/im-operation-chan {:channel (fetch/session (:service db))
                                  :on-success [:imt.auth/store-token loc]
                                  :on-failure [:error/response loc]}}))

(reg-event-fx
 :imt.main/fetch-model
 (sandbox)
 (fn [{db :db} [_ loc]]
   {:db db
    :im-tables/im-operation-chan {:channel (fetch/model (:service db))
                                  :on-success [:imt.main/save-model loc]
                                  :on-failure [:error/response loc]}}))

(reg-event-fx
 :imt.main/fetch-summary-fields
 (sandbox)
 (fn [{db :db} [_ loc]]
   {:db db
    :im-tables/im-operation-chan {:channel (fetch/summary-fields (:service db))
                                  :on-success [:imt.main/save-summary-fields loc]
                                  :on-failure [:error/response loc]}}))

(reg-event-fx
 :imt.auth/store-token
 (sandbox)
 (fn [{db :db} [_ loc token]]
   (let [new-db (assoc-in db [:service :token] token)]
     (cond-> {:db new-db}
       (ready-for-query? new-db)
       (assoc :dispatch [:im-tables/query loc])))))

(reg-event-fx
 :imt.main/save-model
 (sandbox)
 (fn [{db :db} [_ loc model]]
   (let [new-db (assoc-in db [:service :model] model)]
     (cond-> {:db new-db}
       (ready-for-query? new-db)
       (assoc :dispatch [:im-tables/query loc])))))

(reg-event-fx
 :imt.main/save-summary-fields
 (sandbox)
 (fn [{db :db} [_ loc summary-fields]]
   (let [new-db (assoc-in db [:service :summary-fields] summary-fields)]
     (cond-> {:db new-db}
       (ready-for-query? new-db)
       (assoc :dispatch [:im-tables/query loc])))))
