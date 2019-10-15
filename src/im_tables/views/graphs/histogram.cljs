(ns im-tables.views.graphs.histogram)

(defn linear-scale [[r1-lower r1-upper] [r2-lower r2-upper]]
  (fn [v]
    (+ (/ (* (- v r1-lower) (- r2-upper r2-lower))
          (- r1-upper r1-lower))
       r2-lower)))

(defn datum
  "A single scaled bar for the histogram"
  []
  (fn [height-scale idx {:keys [item count]}]
    [:div.histo-bar
     {:style {:height (str (height-scale count) "px")}
      :title (str item ": " count)}]))

(defn main []
  (fn [points]
    (let [only-one-column? (= (count (distinct points)) 1)
          unique-results (reduce (fn [new-set point] (conj new-set (:count point))) #{} points)
          only-one-count-result? (= (count unique-results) 1)
          show-graph? (not (or only-one-column? only-one-count-result?))
          height-scale (linear-scale [0 (apply max (map :count points))] [0 50])]
      (if show-graph?
        (into [:div.graph.histogram]
              (map-indexed (fn [idx d]
                             [datum height-scale idx d (count points)])
                           points))
        (cond only-one-column?
              [:div.no-histogram [:i.fa.fa-bar-chart] " No histogram; only one value in entire column"]
              only-one-count-result?
              [:div.no-histogram [:i.fa.fa-bar-chart] " No histogram; all values occur exactly " (first unique-results) " time(s)"])))))

(defn bucket []
  (fn [bucket-number data-points]
    [:div.histo-bucket
     {:class (if data-points "full" "empty")} max]))

(defn percent-of [min max number]
  (* 100 (/ (- number min) (- max min))))

(def clj-min min)
(def clj-max max)

(defn numerical-histogram []
  (fn [data-points trim]
    (let [{:keys [buckets min max]} (first data-points)
          {:keys [from to]} trim
          by-bucket (group-by :bucket data-points)]
      [:div.histo
       (when (or from to)
         [:div.trimmer
          {:style {:left (str (clj-max 0 (percent-of min max (or from min))) "%")
                   :right (str (clj-min 100 (- 100 (percent-of min max (or to max)))) "%")}}])
       (map (fn [bucket-number]
              ^{:key bucket-number} [bucket bucket-number (get by-bucket bucket-number)]) (range 1 (inc buckets)))])))