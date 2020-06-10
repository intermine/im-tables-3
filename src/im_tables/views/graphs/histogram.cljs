(ns im-tables.views.graphs.histogram
  (:require [reagent.core :as r]
            [im-tables.utils :refer [pretty-number]]))

(defn linear-scale [r1 r2]
  (fn [v]
    (* (/ v r1) r2)))

(defn log-scale [r1 r2]
  (fn [v]
    (* (/ (Math/log v) (Math/log r1)) r2)))

(defn scale [type r1 r2]
  (case type
    :linear (linear-scale r1 r2)
    :log (log-scale r1 r2)))

(defn datum
  "A single scaled bar for the histogram"
  []
  (fn [height-scale idx {:keys [item count]}]
    [:div.histo-bar
     {:style {:height (str (height-scale count) "px")}
      :title (str item ": " count)}]))

(defn main []
  (fn [points scale-type]
    (let [only-one-column? (= (count (distinct points)) 1)
          unique-results (reduce (fn [new-set point] (conj new-set (:count point))) #{} points)
          only-one-count-result? (= (count unique-results) 1)
          show-graph? (not (or only-one-column? only-one-count-result?))
          height-scale (scale scale-type (apply max (map :count points)) 50)]
      (if show-graph?
        (into [:div.graph.histogram]
              (map-indexed (fn [idx d]
                             [datum height-scale idx d (count points)])
                           points))
        (cond only-one-column?
              [:div.no-histogram [:i.fa.fa-bar-chart] " No histogram; only one value in entire column"]
              only-one-count-result?
              [:div.no-histogram [:i.fa.fa-bar-chart] " No histogram; all values occur exactly " (first unique-results) " time(s)"])))))

(defn bucket [height-scale {:keys [bucket min max buckets count] :as data}]
  [:div.histo-bucket
   {:title (when data
             (let [base-amount (/ (- max min) buckets)
                   low-interval (+ (* base-amount (dec bucket)) 364)
                   high-interval (+ (* base-amount bucket) 364)]
               (str (pretty-number low-interval) " to " (pretty-number high-interval)
                    ": " count (if (> count 1) " values" " value"))))
    :class (if data "full" "empty")
    :style (when (number? count)
             ;; Counts of 1 become 0 when scaled logarithmically. They're still
             ;; relevant data points, so we make sure they're 1px tall.
             {:height (str (let [scaled (height-scale count)]
                             (if (zero? scaled) 1 scaled))
                           "px")})}])

(defn percent-of [min max number]
  (* 100 (/ (- number min) (- max min))))

(def clj-min min)
(def clj-max max)

(defn numerical-histogram []
  (let [scale-y (r/atom :linear)]
    (fn [data-points trim]
      (let [{:keys [buckets min max average]} (first data-points)
            {:keys [from to]} trim
            by-bucket (group-by :bucket data-points)
            height-scale (scale @scale-y (apply clj-max (map :count data-points)) 75)]
        [:div.histo-container
         [:div.histo
          (when (or from to)
            [:div.trimmer
             {:style {:left (str (clj-max 0 (percent-of min max (or from min))) "%")
                      :right (str (clj-min 100 (- 100 (percent-of min max (or to max)))) "%")}}])
          [:div.average
           {:title (str "Average: " (pretty-number average))
            :style {:left (str (clj-max 0 (percent-of min max average)) "%")}}]
          (map (fn [bucket-number]
                 ^{:key bucket-number}
                 [bucket height-scale (-> bucket-number by-bucket first)])
               (range 1 (inc buckets)))]
         [:div.histo-x-label
          [:span (pretty-number min)]
          [:div.histo-scale
           [:button.btn.btn-default.btn-xs
            {:type "button"
             :class (when (= @scale-y :linear)
                      "active")
             :on-click #(reset! scale-y :linear)}
            "linear"]
           [:button.btn.btn-default.btn-xs
            {:type "button"
             :class (when (= @scale-y :log)
                      "active")
             :on-click #(reset! scale-y :log)}
            "log"]]
          [:span (pretty-number max)]]]))))
