(ns im-tables.test-utils
  (:require [cljs.core.async :refer [chan put!]]
            [re-frame.core :as rf]))

;; Tips for writing tests:
;; https://github.com/intermine/bluegenes/blob/dev/docs/developing.md

;; Remember to activate the fixtures in your testing namespace if you're
;; going to use exports from this namespace:
;;
;;     (use-fixtures :each utils/fixtures)

(defn stub-fetch-fn
  "We often want to stub imcljs.fetch functions using with-redefs. Instead of
  having to define a function to create, put and return a channel, call this
  function with the value you wish returned and it will do it for you."
  [v]
  (fn [& _]
    (let [c (chan 1)]
      (put! c v)
      c)))

(def stubbed-variables
  "Add functions that reset any globally stubbed variables to this atom.
      (swap! stubbed-variables conj #(set! fetch/session orig-fn))
  A fixture will run all these functions and empty the atom before tests."
  (atom '()))

;; This will be used to clear app-db between test runs.
(rf/reg-event-db
 :clear-db
 (fn [_db] {}))

(def fixtures
  "Necessary fixtures to use the exports from this namespace.
  Use by calling use-fixtures from your testing namespace:
      (use-fixtures :each utils/fixtures)"
  {:before (fn []
             (when-let [vars (seq @stubbed-variables)]
               (doseq [restore-fn vars]
                 (restore-fn))
               (reset! stubbed-variables '()))
             (rf/dispatch-sync [:clear-db]))})

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
