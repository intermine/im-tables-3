(ns im-tables.interceptors
  (:require [re-frame.core :refer [->interceptor]]))

(defn sandbox
  "Returns an interceptor factory that shrinks the world
   down to a subset of app-db. Changes are merged back into
   the real db when the handler finishes at a path supplied
   as the first argument in an event (or second argument if you count the name of the event as an arg).
  (dispatch [:some-event some-data [:my :sandboxed :location])"
  []
  (->interceptor
   :id :sandbox
   :before (fn [context]
              ;(.log js/console "before" context)
             (if-let [path (second (get-in context [:coeffects :event]))]
               (do
                  ;(println "before found path" path)

                 (update context :coeffects assoc
                         :old-db (get-in context [:coeffects :db])
                         :db (get-in context (concat [:coeffects :db] path))))
               context))
   :after (fn [context]
            #_(.log js/console "after" (if-let [path (second (get-in context [:coeffects :event]))]
                                         (let [old-db (get-in context [:coeffects :old-db])
                                               new-db (get-in context [:effects :db])]
                                           (assoc-in context [:effects :db] (assoc-in old-db path new-db)))
                                         context))
            (if-let [path (second (get-in context [:coeffects :event]))]
              (let [old-db (get-in context [:coeffects :old-db])
                    new-db (get-in context [:effects :db])]
                (assoc-in context [:effects :db] (assoc-in old-db path new-db)))
              context))))
