(ns im-tables.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [imcljs.path :as path]
            [clojure.string :as string]))

(defn glue [path remainder-vec]
  (reduce conj (or path []) remainder-vec))

(reg-sub
  :main/query-response
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:response]))))

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
  :settings/data-out
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:settings :data-out]))))

(reg-sub
  :settings/settings
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:settings]))))

(reg-sub
  :summaries/column-summaries
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:cache :column-summary]))))

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
  (coll-head-contains? [1 2] [1 2 3 4]) => true
  Strings are collections of characters, so this function also mimics clojure.string/starts-with?
  (coll-head-contains? apple applejuice) => true"
  [sub-coll coll]
  (every? true? (map = sub-coll coll)))

(def head-missing? (complement head-contains?))

(defn group-by-starts-with
  "Given a substring and a collection of strings, shift all occurrences
  of strings beginning with that substring to immediately follow the first occurrence
  ex: (group-by-starts-with [orange apple banana applepie apricot applejuice] apple)
  => [orange apple applepie applejuice banana apricot]"
  [string-coll starts-with]
  (let [leading (take-while (partial head-missing? starts-with) string-coll)]
    (concat leading
            (filter (partial head-contains? starts-with) string-coll)
            (filter (partial head-missing? starts-with) (drop (count leading) string-coll)))))

(defn replace-join-views
  "Remove all occurrences of strings in a collection that begin with a value while
   replacing the first occurrences of the match with the value
   ex: (replace-join-views [orange apple applepie applejuice banana apricot] apple)
   => [orange apple banana apricot]"
  [string-coll starts-with]
  (let [leading (take-while (partial head-missing? starts-with) string-coll)]
    (concat leading
            [starts-with]
            (filter (partial head-missing? starts-with) (drop (count leading) string-coll)))))

(defn collection?
  "Does the provided path represent a collection property of a class?"
  [model path]
  (when-let [walked (path/walk model path)]
    (let [path-end (keyword (last (string/split path ".")))
          [parent child] (take-last 2 walked)]
      (contains? (:collections parent) path-end))))


(reg-sub
  :query-response/views
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:response :views]))))

(reg-sub
  :query/joins
  (fn [db [_ prefix]]
    (get-in db (glue prefix [:query :joins]))))

; The following two subscriptions do two things to support outer joins, resulting in:
;     Gene.secondaryIdentifier Gene.publications.year Gene.symbol Gene.publications.title
; ... with outer joins [Gene.publications]
; Becoming:
;     Gene.secondaryIdentifier Gene.publications Gene.symbol

; This could have been done with a single subscription but having a reference
; to the grouped views [:query-response/views-sorted-by-joins] is useful elsewhere

; First move any views that are part of outer joins next to eachother:
(reg-sub
  :query-response/views-sorted-by-joins
  (fn [[_ loc]]
    [(subscribe [:query-response/views loc])
     (subscribe [:query/joins loc])
     (subscribe [:assets/model loc])])
  (fn [[views joins model]]
    (when (and model (js/console.log "D" (map (partial collection? model) joins))))
    (reduce (fn [total next] (group-by-starts-with total next)) views (filter (partial collection? model) joins))))

; ...then replace all views that are part of outer joins with the name of the outer joins:
(reg-sub
  :query-response/views-collapsed-by-joins
  (fn [[_ loc]]
    [(subscribe [:query-response/views-sorted-by-joins loc])
     (subscribe [:query/joins loc])
     (subscribe [:assets/model loc])])
  (fn [[views joins model]]
    (reduce (fn [total next] (replace-join-views total next)) views (filter (partial collection? model) joins))))

(reg-sub
  :rel-manager/query
  (fn [db [_ loc]]
    (get-in db (glue loc [:cache :rel-manager]))))


