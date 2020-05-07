(ns im-tables.utils
  (:require [oops.core :refer [ocall]]
            [inflections.core :refer [plural]]
            [imcljs.path :as path]
            [clojure.string :as string]
            [goog.json :as json]
            [goog.i18n.NumberFormat.Format])
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

(defn path->displaynames
  "Takes a path as the `view` argument and returns the corresponding vector of
  display names. Will prioritise the displayName of the referencedType class
  (instead of the displayName in the references/collections/attributes map).
  This makes a difference with eg. `Gene.dataSets.name`, where
  `classes.Gene.collections.dataSets.displayName` is `Data Sets` while
  `classes.DataSet.displayName` is `Data Set`."
  [model view]
  (let [[head & tail] (path/split-path view)]
    (loop [names []
           paths tail
           class (get-in model [:classes head])]
      (let [new-names (conj names (:displayName class))]
        (if (seq paths)
          (recur new-names
                 (next paths)
                 (let [subclasses (apply merge
                                         ((juxt :references :collections :attributes) class))
                       subclass   (subclasses (first paths))]
                   (if-let [reference (:referencedType subclass)]
                     (get-in model [:classes (keyword reference)])
                     subclass)))
          new-names)))))

(defn display-name
  "Takes a view path and returns a human-readable name string."
  [model view]
  (->> (path->displaynames model view)
       (take-last 2) ;; Last two names of the path are most descriptive.
       (string/join " ")
       plural))

(defn response->error
  "Convert an HTTP response to an error map."
  [response]
  (let [error-msg (or (get-in response [:body :error])
                      (:body response))
        error-type (if (string? error-msg) :query :network)]
    {:type error-type
     :message (when (= error-type :query) error-msg)
     :response response}))

(defn constraints->logic
  "Generates the default 'A and B and ...' constraint logic string from a
  sequence of constraints. Note that `:code` needs to be present on all
  constraint maps."
  [constraints]
  (->> (map :code constraints)
       (string/join " and ")))

(defn clj->json
  "Converts a Clojure data structure to JSON."
  [x]
  (some-> x
          (clj->js)
          (json/serialize)))
