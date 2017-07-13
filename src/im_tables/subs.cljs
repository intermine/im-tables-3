(ns im-tables.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub]]))

(defn glue [path remainder-vec]
  (reduce conj (or path []) remainder-vec))

(reg-sub
  :main/query-response
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query-response]))))

(reg-sub
  :main/query
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query]))))

(reg-sub
  :main/temp-query
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:temp-query]))))

(reg-sub
  :main/query-parts
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query-parts]))))

(reg-sub
  :style/overlay?
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :overlay?]))))

(reg-sub
  :summary/item-details
  (fn [db [_ loc id]]
    (get-in db (glue loc [:cache :item-details id]))))

(reg-sub
  :style/dragging-item
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :dragging-item]))))

(reg-sub
  :style/dragging-over
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :dragging-over]))))

(reg-sub
  :settings/pagination
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:settings :pagination]))))

(reg-sub
  :settings/settings
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:settings]))))

(reg-sub
  :summaries/column-summaries
  (fn [db [_ prefix]]
    (get-in db [:cache :column-summary])))

(reg-sub
  :selection/selections
  (fn [db [_ prefix view]]
    (get-in db (glue prefix [:cache :column-summary view :selections]))))

(reg-sub
  :selection/response
  (fn [db [_ prefix view]]
    (get-in db (glue prefix [:cache :column-summary view :response]))))

(reg-sub
  :selection/text-filter
  (fn [db [_ prefix view]]
    (get-in db (glue prefix [:cache :column-summary view :filters :text]))))

(reg-sub
  :assets/model
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:service :model]))))

(reg-sub
  :tree-view/selection
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :tree-view :selection]))))

(reg-sub
  :modal
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :modal]))))


(defn head-contains?
  "True if a collection's head contains all elements of another collection (sub-coll)
  (coll-head-contains? [1 2] [1 2 3 4]) => true"
  [sub-coll coll]
  (every? true? (map = sub-coll coll)))

(def head-missing? (complement head-contains?))

(defn group-by-starts-with
  "Given a substring and a collection of strings, shift all occurences
  of strings beginning with that substring to immediately follow the first occurence
  ex: (group-by-starts-with apple [orange apple banana applepie apricot applejuice])
  => [orange apple applepie applejuice banana apricot]"
  [string-coll starts-with]
  (let [leading (take-while (partial head-missing? starts-with) string-coll)]
    (concat leading
            (filter (partial head-contains? starts-with) string-coll)
            (filter (partial head-missing? starts-with) (drop (count leading) string-coll)))))

(defn replace-join-views
  [string-coll starts-with]
  (let [leading (take-while (partial head-missing? starts-with) string-coll)]
    (concat leading
            [starts-with]
            (filter (partial head-missing? starts-with) (drop (count leading) string-coll)))))

(reg-sub
  :query-response/views
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query-response :views]))))

(reg-sub
  :query/joins
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query :joins]))))

(reg-sub
  :query-response/views-sorted-by-joins
  :<- [:query-response/views]
  :<- [:query/joins]
  (fn [[views joins]]
    (reduce (fn [total next] (group-by-starts-with total next)) views joins)))

(reg-sub
  :query-response/views-collapsed-by-joins
  :<- [:query-response/views-sorted-by-joins]
  :<- [:query/joins]
  (fn [[views joins]]
    (reduce (fn [total next] (replace-join-views total next)) views joins)))