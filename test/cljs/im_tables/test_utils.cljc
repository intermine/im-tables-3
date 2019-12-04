(ns im-tables.test-utils
  (:require [day8.re-frame.test :refer [run-test-sync run-test-async wait-for]]
            [re-frame.core :as rf]
            [clojure.string :as string]
            #?(:cljs [cljs.core.async :refer [chan put!]]))
  #?(:cljs (:require-macros [im-tables.test-utils])))

;; Tips for writing tests:
;; https://github.com/intermine/bluegenes/blob/dev/docs/developing.md

;; Remember to activate the fixtures in your testing namespace if you're
;; going to use exports from this namespace:
;;
;;     (use-fixtures :each utils/fixtures)

#?(:cljs
   (defn stub-fetch-fn
     "We often want to stub imcljs.fetch functions using with-redefs. Instead of
     having to define a function to create, put and return a channel, call this
     function with the value you wish returned and it will do it for you."
     [v]
     (fn [& _]
       (let [c (chan 1)]
         (put! c v)
         c))))

#?(:cljs
   (def stubbed-variables
     "Add functions that reset any globally stubbed variables to this atom.
     (swap! stubbed-variables conj #(set! fetch/session orig-fn))
     A fixture will run all these functions and empty the atom before tests."
     (atom '())))

;; This will be used to clear app-db between test runs.
#?(:cljs
   (rf/reg-event-db
    :clear-db
    (fn [_db] {})))

#?(:cljs
   (def fixtures
     "Necessary fixtures to use the exports from this namespace.
     Use by calling use-fixtures from your testing namespace:
     (use-fixtures :each utils/fixtures)"
     {:before (fn []
                (when-let [vars (seq @stubbed-variables)]
                  (doseq [restore-fn vars]
                    (restore-fn))
                  (reset! stubbed-variables '()))
                (rf/dispatch-sync [:clear-db]))}))

(defn- events-matched?
  [matches matchm]
  (every? (fn [[event-id times]]
            (= (get matches event-id) times))
          matchm))

(defn match-times
  "For use inside `(day8.re-frame.test/wait-for [(match-times matchm)])`.
  Expects an event-id matching count map as argument:
      {:main/save-column-summary 6
       :main/save-decon-count    3}
  Waits until all event-id keys in the map have occurred the exact amount of
  times specified as the value. Useful when you know a chain dispatch can fire
  an X amount of requests, and you just have to wait until they all finish."
  [matchm]
  (let [matches (atom nil)]
    (fn [[event-id]]
      (events-matched? (swap! matches update event-id (fnil inc 0)) matchm))))

(defn deconstruct-naively
  "Works similarly to `imcljs.query/deconstruct-by-class` except it doesn't
  need a model to deconstruct. In addition, it returns only a hash set of the
  unique classes instead of a map with more data. For when you need a solid
  guess at the amount of deconstruction operations for a query."
  [{:keys [from select] :as _query}]
  (let [full-paths (map #(if (string/starts-with? % from)
                           %
                           (str from "." %))
                        select)]
    (into #{}
          (comp (map #(string/join "." (drop-last (string/split % #"\."))))
                (filter some?))
          full-paths)))

;; Here are some simple macros that expand to code you'll often need to write
;; in tests. By grouping them under a single name for each operation, the unit
;; tests can be written more succinctly, making their intent clearer.

(defmacro after-load
  "Common operation that includes loading im-tables-3."
  [location im-config & body]
  `(run-test-async
    (rf/dispatch-sync [:im-tables/load ~location ~im-config])
    (wait-for [:main/replace-query-response]
      ~@body)))
