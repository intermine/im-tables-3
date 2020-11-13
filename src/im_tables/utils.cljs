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
