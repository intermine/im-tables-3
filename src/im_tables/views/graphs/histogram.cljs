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
          [:div.no-histogram [:i.fa.fa-bar-chart] " No histogram; all values occur exactly "(first unique-results) " time(s)"])))))
