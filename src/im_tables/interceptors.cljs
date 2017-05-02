(ns im-tables.interceptors
  (:require [re-frame.core :refer [->interceptor]]))

(defn sandbox
  "Returns an interceptor factory that shrinks the world
   down to a subset of app-db. Changes are merged back into
   the real db when the handler finishes at a path supplied
   as the last argument in an event.
  (dispatch [:some-event some-data [:my :sandboxed :location])"
  []
  (->interceptor
    :id :sandbox
    :before (fn [context]
              ;(.log js/console "before" context)
              ;;TODO NOTE: If the second arg in an event dispatch is an HTML element or event target, this method will try to seq over the element and thow an unSeqable error. To workaround, pass it as a second or third arg?
              ;;further musing: Should the "second" method below be last instead based on the comment at the top? Ask Josh.
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
