(ns im-tables.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [clojure.string :as string]
            [imcljs.internal.utils :refer [scrub-url]]
            [imcljs.query :as im-query]))

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
 :main/query-constraints
 (fn [[_ prefix]]
   (subscribe [:main/query prefix]))
 (fn [query [_ _prefix]]
   (:where query)))

(reg-sub
 :main/temp-query
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:temp-query]))))

(reg-sub
 :main/query-parts
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:query-parts]))))

(reg-sub
 :main/query-parts-counts
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:query-parts-counts]))))

(reg-sub
 :main/error
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:error]))))

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
 :settings/cdn
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:settings :cdn]))))

(reg-sub
 :settings/compact
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:settings :compact]))))

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
 :selection/possible-values
 (fn [db [_ prefix view]]
   (get-in db (glue prefix [:cache :possible-values view]))))

(reg-sub
 :assets/service
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:service]))))

;; We add `:type-constraints` to the model right here in case it is used to
;; traverse a subclass. In some places the constraints of `:temp-query` will be
;; used instead, in which case they assoc `:type-constraints` themselves.
(reg-sub
 :assets/model
 (fn [[_ prefix]]
   [(subscribe [:assets/service prefix])
    (subscribe [:main/query-constraints prefix])])
 (fn [[service constraints] [_ _prefix]]
   (assoc (:model service)
          :type-constraints constraints)))

(reg-sub
 :tree-view/selection
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:cache :tree-view :selection]))))

(reg-sub
 :exporttable/preview
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:cache :export-preview]))))

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
  "Given a substring and a collection of strings, shift all occurences
  of strings beginning with that substring to immediately follow the first occurence
  ex: (group-by-starts-with [orange apple banana applepie apricot applejuice] apple)
  => [orange apple applepie applejuice banana apricot]"
  [string-coll starts-with]
  (let [leading (take-while (partial head-missing? starts-with) string-coll)]
    (concat leading
            (filter (partial head-contains? starts-with) string-coll)
            (filter (partial head-missing? starts-with) (drop (count leading) string-coll)))))

(defn replace-join-views
  "Remove all occurances of strings in a collection that begin with a value while
   replacing the first occurance of the match with the value
   ex: (replace-join-views [orange apple applepie applejuice banana apricot] apple)
   => [orange apple banana apricot]"
  [string-coll starts-with]
  (let [leading (take-while (partial head-missing? starts-with) string-coll)]
    (concat leading
            [starts-with]
            (filter (partial head-missing? starts-with) (drop (count leading) string-coll)))))

(reg-sub
 :query-response/views
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:response :views]))))

(reg-sub
 :query/joins
 (fn [db [_ prefix]]
   (get-in db (glue prefix [:query :joins]))))

(reg-sub
 :query/joined-path?
 (fn [[_ loc]]
   (subscribe [:query/joins loc]))
 (fn [joins [_ _prefix path]]
   (contains? (set joins) path)))

;; Map from outer joined paths in the query to views descendent of that path.
;; e.g. {"Gene.interactions" ["Gene.interactions.participant2.alleles.primaryIdentifier"
;;                            "Gene.interactions.participant2.alleles.symbol"
;;                            "Gene.interactions.participant2.alleles.alleleClass"
;;                            "Gene.interactions.participant2.alleles.organism.name"]}
(reg-sub
 :query-response/joins->views
 (fn [[_ loc]]
   [(subscribe [:query/joins loc])
    (subscribe [:query-response/views loc])])
 (fn [[joins views]]
   (reduce (fn [m view]
             (if-let [matched-join (some #(when (string/starts-with? view (str % ".")) %) joins)]
               (update m matched-join (fnil conj []) view)
               m))
           {}
           views)))

(reg-sub
 :query-response/joined-views
 (fn [[_ loc]]
   (subscribe [:query-response/joins->views loc]))
 (fn [joins->views [_ _prefix path]]
   (get joins->views path)))

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
    (subscribe [:query/joins loc])])
 (fn [[views joins]]
   (reduce (fn [total next] (group-by-starts-with total next)) views joins)))

; ...then replace all views that are part of outer joins with the name of the outer joins:
(reg-sub
 :query-response/views-collapsed-by-joins
 (fn [[_ loc]]
   [(subscribe [:query-response/views-sorted-by-joins loc])
    (subscribe [:query/joins loc])])
 (fn [[views joins]]
   (reduce (fn [total next] (replace-join-views total next)) views joins)))

(reg-sub
 :rel-manager/query
 (fn [db [_ loc]]
   (get-in db (glue loc [:cache :rel-manager]))))

;;;;;;;;;;;;;;;; Code generation

(defn generate-javascript [{:keys [cdn query service html? comments?]}]
  (string/join "\n"
               (-> []
                   (cond->
                    (and html? comments?) (conj "<!-- The Element we will target -->")
                    html? (conj "<div id=\"some-elem\"></div>\n")
                    (and html? comments?) (conj "<!-- The imtables source -->")
                    html? (conj (str "<script src=\"" cdn "/js/intermine/im-tables/latest/imtables.js\" charset=\"UTF8\"></script>"))
                    html? (conj (str "<link href=\"" cdn "/js/intermine/im-tables/latest/main.sandboxed.css\" rel=\"stylesheet\">\n"))
                    html? (conj "<script>")

                    (and (not html?) comments?) (conj "/* Install from npm: npm install imtables\n * This snippet assumes the presence on the page of an element like:\n * <div id=\"some-elem\"></div>\n */")
                    (not html?) (conj "var imtables = require(\"imtables\");\n"))
                   (conj "var selector = \"#some-elem\";\n")
                   (conj (str "var service = " (js/JSON.stringify
                                                (clj->js (-> service
                                                             (select-keys [:root :token])
                                                             (update :root scrub-url))) nil 2) "\n"))
                   (conj (str "var query = " (js/JSON.stringify (clj->js query) nil 2) "\n"))
                   (conj (str "imtables.loadTable(\n  selector, // Can also be an element, or a jQuery object.\n  {\"start\":0,\"size\":25}, // May be null\n  {service: service, query: query} // May be an imjs.Query\n).then(\n  function (table) { console.log('Table loaded', table); },\n  function (error) { console.error('Could not load table', error); }\n);"))
                   (cond->
                    html? (conj "</script>")))))

(defn remove-java-comments [s]
  (clojure.string/replace s #"/\*([\S\s]*?)\*/" ""))

(defn octo-comment? [s]
  (or
   (clojure.string/starts-with? s "# ")
   (every? true? (map (partial = "#") s))))

(def not-octo-comment? (complement octo-comment?))

(defn remove-octothorpe-comments [s]
  (let [lines (clojure.string/split-lines s)]
    (clojure.string/replace
     (->> lines
          (map (fn [line] (if (octo-comment? line) "\n" line)))
          (clojure.string/join "\n"))
     #"\n\n\n+"
     "\n\n")))

(defn format-code [{:keys [lang code comments? html? query service cdn] :as options}]
  (cond
    (= "js" lang) (when (and query service) (generate-javascript options))
    (= "java" lang) (cond-> code (not comments?) remove-java-comments)
    (= "xml" lang) code
    (or
     (= "rb" lang)
     (= "py" lang)
     (= "pl" lang)) (cond-> code (not comments?) remove-octothorpe-comments)
    :else ""))

(reg-sub
 :codegen/code
 (fn [db [_ loc]]
   (get-in db (glue loc [:codegen :code]))))

(reg-sub
 :codegen/options
 (fn [db [_ loc]]
   (get-in db (glue loc [:settings :codegen]))))

(reg-sub
 :codegen/formatted-code
 (fn [[_ loc]]
   [(subscribe [:settings/cdn loc])
    (subscribe [:codegen/code loc])
    (subscribe [:codegen/options loc])
    (subscribe [:main/query loc])
    (subscribe [:assets/service loc])])
 (fn [[cdn code options query service] [_ _loc]]
   (let [{:keys [html? comments? lang]} options]
     (when code
       (format-code {:lang lang
                     :code code
                     :comments? comments?
                     :html? html?
                     :query query
                     :service service
                     :cdn cdn})))))

(reg-sub
 :ui/column-sort-direction
 (fn [db [_ loc view]]
   (let [sortm (get-in db (glue loc [:query :sortOrder 0]))]
     (when (and sortm (= (string/join "." (drop 1 (string/split view ".")))
                         (:path sortm)))
       (:direction sortm)))))

(reg-sub
 :filter-manager/filters
 (fn [[_ loc]]
   (subscribe [:main/temp-query loc]))
 (fn [query [_ loc]]
   (:where query)))

(reg-sub
 :filter-manager/constraint-logic
 (fn [[_ loc]]
   (subscribe [:main/temp-query loc]))
 (fn [query [_ loc]]
   (:constraintLogic query)))

(reg-sub
 :pick-items/picked
 (fn [db [_ loc]]
   (get-in db (glue loc [:pick-items :picked]))))

(reg-sub
 :pick-items/is-picked?
 (fn [[_ loc]]
   (subscribe [:pick-items/picked loc]))
 (fn [picked [_ _loc id]]
   (contains? picked id)))

(reg-sub
 :pick-items/class
 (fn [db [_ loc]]
   (get-in db (glue loc [:pick-items :class]))))

(reg-sub
 :export/download-href
 (fn [[_ loc]]
   [(subscribe [:settings/data-out loc])
    (subscribe [:assets/service loc])
    (subscribe [:main/query loc])
    (subscribe [:assets/model loc])])
 (fn [[{:keys [filename format columnheaders size start select remove export-data-package compression]} {:keys [root token]} query model]
      [_ _loc]]
   (let [sequence? (contains? #{"fasta" "gff3" "bed"} format)]
     (str root "/service/query/results" (when sequence? (str "/" format))
          "?format=" format
          "&filename=" (or filename "results")
          "&query=" (js/encodeURIComponent
                     (im-query/->xml model (cond
                                             sequence? (assoc query :select ["id"])
                                             select (assoc query :select (vec (keep #(when-not (contains? remove %) %) select)))
                                             :else query)))
          (when size (str "&size=" size))
          (when start (str "&start=" start))
          (when columnheaders
            (str "&columnheaders=" columnheaders))
          (if export-data-package
            "&exportDataPackage=true&compress=zip"
            (when compression
              (str "&compress=" compression)))
          "&token=" token))))
