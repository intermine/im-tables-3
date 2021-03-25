(ns im-tables.utils
  (:require [oops.core :refer [ocall oget]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path]
            [clojure.string :as string]
            [goog.json :as json]
            [goog.i18n.NumberFormat.Format]
            [goog.style :as gstyle]
            [goog.dom :as gdom])
  (:import [goog.i18n NumberFormat]
           [goog.i18n.NumberFormat Format]))

(let [numformat (NumberFormat. Format/DECIMAL)]
  (defn pretty-number
    "Returns a human-readable (comma thousand-separators) number using the
    Google Closure formatter."
    [n]
    (.format numformat (str n))))

(defn on-event
  "For use with `:ref` attribute on elements to easily define jquery listeners.
  Uses `some->` as the event isn't guaranteed to hold a value. Returns the event
  so you can chain multiple `on` calls by wrapping them in `comp`. Example:
      [:span.dropdown
       {:ref (comp
               (on \"hide.bs.dropdown\"
                   #(js/alert \"I'm closing!\"))
               (on \"show.bs.dropdown\"
                   #(js/alert \"I'm opening!\")))}]"
  [trigger callback]
  (fn [event]
    (some-> event
            js/$
            (ocall :off trigger)
            (ocall :on trigger callback))
    event))

(defn display-name
  "Takes a view path and returns a human-readable name string."
  [model view]
  (->> (path/walk model view)
       (map :displayName)
       (take-last 2) ;; Last two names of the path are most descriptive.
       (string/join " ")
       plural))

(defn response->error
  "Convert an HTTP response to an error map."
  [response]
  (let [error-msg (or (get-in response [:body :error])
                      (:body response))
        error-type (cond
                     (nil? response)            :network
                     (< (:status response) 500) :query
                     :else                      :network)]
    {:type error-type
     :message (when (string? error-msg) error-msg)
     :response response}))

(defn constraints->logic
  "Generates the default 'A and B and ...' constraint logic string from a
  sequence of constraints. Note that `:code` needs to be present on all
  applicable constraint maps, which `imcljs.query/sterilize-query` ensures."
  [constraints]
  (->> (keep :code constraints)
       (string/join " and ")))

(defn clj->json
  "Converts a Clojure data structure to JSON."
  [x]
  (some-> x
          (clj->js)
          (json/serialize)))

(defn clean-derived-query
  "New queries are sometimes derived from an existing query, to fulfill an
  auxilliary purpose. The InterMine backend will in some cases return 'Service
  failed' if a query specifies sorting for a column that isn't part of the
  view, or joins that isn't relevant to the results. In most cases, these
  properties won't have an effect on the result and can safely be removed."
  [query]
  (dissoc query
          :joins :sortOrder :orderBy))

(defn rows->maps
  "Takes an `imcljs.fetch/rows` response and transforms it into a vector of
  maps, with the last portion of the path as keyword keys ('Gene.symbol' -> :symbol)."
  [res]
  (let [views (map (comp keyword #(re-find #"[^\.]+$" %)) (:views res))]
    (mapv (fn [result]
            (zipmap views result))
          (:results res))))

;; The 360 magic number used below is the minimum width for the column summary.
;; Instead of updating the position of the dropdown when it's done loading, I
;; went the simple route of guessing the position to be good enough. This means
;; - when dropdown is on right half of screen, the loader is not correctly
;;   aligned, but when it transitions to showing the data, it will be
;;   - it will however not be if the width of the dropdown is greater
;; - once the data is loaded, any subsequent dropdown opened will be aligned
(defn place-below!
  "Call with DOM elements to set the position styling of `below` such that it
  is directly below the edge of `above`.
  `right?` - position `below` using its top right corner (instead of top left).
  `loading?` - when `right?` is true, we need to know the width of `below` to
               position it correctly. This arg tells us that the width might
               change when it's done loading, so we just guess instead."
  [below above & {:keys [right? loading?]}]
  (let [[above-w offset-y] ((juxt #(oget % :width) #(oget % :height))
                            (gstyle/getSize above))
        offset-x (if right?
                   (* -1 (- (if loading?
                              360
                              (oget (gstyle/getSize below) :width))
                            above-w))
                   0)
        pos (-> (gstyle/getRelativePosition above (gdom/getAncestorByClass above "im-table"))
                (ocall :translate offset-x offset-y))]
    (gstyle/setPosition below pos)))
