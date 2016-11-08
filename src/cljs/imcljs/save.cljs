(ns imcljs.save
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs-http.client :as http]
            [imcljs.utils :as utils :refer [cleanse-url]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))


(defn wrap-auth [service request-map]
  (if-let [token (get service :token)]
    (assoc-in request-map [:headers "authorization"] (str "Token " token))
    request-map))




(defn query-to-list
  "Returns IMJS row-style result"
  [service query & [options]]
  (let [c (chan)]
    (-> (js/imjs.Service. (clj->js service))
        (.query (clj->js query))
        (.then (fn [q]
                 (go (let [root     (utils/cleanse-url (:root service))
                           response (<! (http/post (str root "/query/tolist")
                                                   (wrap-auth service
                                                              {:with-credentials?
                                                               false
                                                               :form-params
                                                               (merge {:format "json"}
                                                                      options
                                                                      {:query (.toXML q)})})))]
                       (>! c (-> response :body))
                       (close! c))))
               (fn [error]
                 (println "ERROR" error))))
    c))