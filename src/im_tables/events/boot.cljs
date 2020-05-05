(ns im-tables.events.boot
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [re-frame.core :refer [reg-event-fx reg-event-db]]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [im-tables.db :as db]
            [im-tables.interceptors :refer [sandbox]]
            [imcljs.fetch :as fetch]
            [imcljs.auth :as auth]
            [imcljs.query :as im-query]))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn boot-flow
  [loc]
  {:first-dispatch [:imt.auth/init loc]
   :rules [{:when :seen?
            :events [:imt.auth/store-token]
            :dispatch-n [[:imt.main/fetch-model loc]
                         [:imt.main/fetch-summary-fields loc]]}
           {:when :seen-all-of?
            :events [:imt.main/save-model
                     :imt.main/save-summary-fields]
            :dispatch [:im-tables/query loc]
            :halt? true}]})
;; TODO handle failures in the three fetch

(reg-event-fx
 :im-tables/load
 (sandbox)
 (fn [{db :db} [_ loc {:keys [query service location response settings] :as args}]]
   {:db (assoc args :settings (deep-merge (:settings db/default-db) settings))
    :async-flow (boot-flow loc)}))

(reg-event-fx
 :im-tables/reload
 (sandbox)
 (fn [{db :db} [_ loc]]
   {:db db
    :async-flow (boot-flow loc)}))

;; Suggestion: Re-use `:im-tables.main/run-query` instead, if suitable.
(reg-event-fx
 :im-tables/query
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [service (:service db)
         {:keys [pagination buffer]} (:settings db)
         {:keys [start limit]}       pagination
         query (im-query/sterilize-query (:query db))]
     {:db (assoc db
                 :query (cond-> query
                          (not (contains? query :constraintLogic))
                          (assoc :constraintLogic (->> (:where query)
                                                       (map :code)
                                                       (str/join " and ")))))
      :dispatch [:main/deconstruct loc]
      :im-tables/im-operation-chan
      {:channel (fetch/table-rows service query {:start start
                                                 :size (* limit buffer)})
       :on-success ^:flush-dom [:main/replace-query-response loc pagination]
       :on-failure ^:flush-dom [:error/network loc]}})))

(reg-event-db
 :initialize-db
 (sandbox)
 (fn [_] db/default-db))

;; Suggestion: Instead of verifying that the token is valid, we could trust
;; that it is, to save an extraneous HTTP request (we would instead have to
;; handle getting a token at a later point, should it turn out to be invalid).
(reg-event-fx
 :imt.auth/init
 (sandbox)
 (fn [{db :db} [_ loc]]
   (let [token (get-in db [:service :token])]
     (merge
      {:db db}
      (if (empty? token)
        {:dispatch [:imt.auth/fetch-anonymous-token loc]}
        {:im-tables/im-operation-chan
         {:channel (auth/who-am-i? (:service db) token)
          :on-success [:imt.auth/store-token loc token]
          :on-failure [:imt.auth/fetch-anonymous-token loc]}})))))

; Fetch an anonymous token for a give
(reg-event-fx
 :imt.auth/fetch-anonymous-token
 (sandbox)
 (fn [{db :db} [_ loc]]
   {:db db
    :im-tables/im-operation-chan
    {:channel (fetch/session (:service db))
     :on-success [:imt.auth/store-token loc]
     :on-failure [:error/network loc]}}))

; Store an auth token for a given mine
(reg-event-db
 :imt.auth/store-token
 (sandbox)
 (fn [db [_ loc token]]
   (assoc-in db [:service :token] token)))

(reg-event-fx
 :imt.main/fetch-model
 (sandbox)
 (fn [{db :db} [_ loc]]
   (merge
    {:db db}
    (if (get-in db [:service :model])
      {:dispatch [:imt.main/save-model loc]}
      {:im-tables/im-operation-chan
       {:channel (fetch/model (:service db))
        :on-success [:imt.main/save-model loc]
        :on-failure [:error/network loc]}}))))

(reg-event-fx
 :imt.main/fetch-summary-fields
 (sandbox)
 (fn [{db :db} [_ loc]]
   (merge
    {:db db}
    (if (get-in db [:service :summary-fields])
      {:dispatch [:imt.main/save-summary-fields loc]}
      {:im-tables/im-operation-chan
       {:channel (fetch/summary-fields (:service db))
        :on-success [:imt.main/save-summary-fields loc]
        :on-failure [:error/network loc]}}))))

(reg-event-db
 :imt.main/save-model
 (sandbox)
 (fn [db [_ loc model]]
   (cond-> db
     (not-empty model)
     (assoc-in [:service :model] model))))

(reg-event-db
 :imt.main/save-summary-fields
 (sandbox)
 (fn [db [_ loc summary-fields]]
   (cond-> db
     (not-empty summary-fields)
     (assoc-in [:service :summary-fields] summary-fields))))
