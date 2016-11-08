(ns imcljs.search
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-http.client :as http]
            [imcljs.utils :as utils :refer [cleanse-url query->xml]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))


(defn build-feature-query [regions]
  {:from   "SequenceFeature"
   :select ["SequenceFeature.id"
            "SequenceFeature.name"
            "SequenceFeature.primaryIdentifier"
            "SequenceFeature.symbol"
            "SequenceFeature.chromosomeLocation.*"]
   :where  [{:path   "SequenceFeature.chromosomeLocation"
             :op     "OVERLAPS"
             :values (if (string? regions) [regions] (into [] regions))}]})

(defn quicksearch
  "Returns the results of a quicksearch"
  [{root :root token :token} term]
  (let [root (utils/cleanse-url root)]
    (go (:results (:body (<! (http/get (str root "/search")
                                       {:query-params      {:q    term
                                                            :size 5}
                                        :with-credentials? false})))))))

(defn raw-query-rows
  "Returns IMJS row-style result"
  [service query & [options]]
  (let [c (chan)]
    (-> (js/imjs.Service. (clj->js service))
        (.query (clj->js query))
        (.then (fn [q]
                 (go (let [root (utils/cleanse-url (:root service))
                           response (<! (http/post (str root "/query/results")
                                                   {:with-credentials? false
                                                    :form-params       (merge {:format "json"} options {:query (.toXML q)})}))]
                       (>! c (-> response :body))
                       (close! c))))
               (fn [error]
                 (println "ERROR" error))))
    c))

(defn path-values
  "Returns IMJS row-style result"
  [service query & [options]]
  (let [c (chan)]
    (-> (js/imjs.Service. (clj->js service))
        (.query (clj->js query))
        (.then (fn [q]
                 (go (let [root (utils/cleanse-url (:root service))
                           response (<! (http/post (str root "/path/values")
                                                   {:with-credentials? false
                                                    :form-params       (merge options {:query (.toXML q)})}))]
                       (>! c (-> response :body))
                       (close! c))))
               (fn [error]
                 (println "path values error" error))))
    c))

(defn table-rows
  "Returns IMJS row-style result"
  [service model query & [options]]
  (let [c (chan)]
    (go (let [root (utils/cleanse-url (:root service))
              response (<! (http/post (str root "/query/results/tablerows")
                                      {:with-credentials? false
                                       :form-params       (merge {:format "json"} options {:query (query->xml model query)})}))]
          (>! c (-> response :body))
          (close! c)))
    c))


(defn enrichment
  "Get the results of using a list enrichment widget to calculate statistics for a set of objects."
  [{root :root token :token} {:keys [ids list widget maxp correction population]}]
  (go (:body (<! (http/post
                   (str (cleanse-url root) "/list/enrichment")
                   {:with-credentials? false
                    :keywordize-keys?  true
                    :form-params       (merge {:widget     widget
                                               :maxp       maxp
                                               :format     "json"
                                               :correction correction}
                                              (cond
                                                ids {:ids (clojure.string/join "," ids)}
                                                list {:list list})
                                              (if-not (nil? population) {:population population}))})))))
