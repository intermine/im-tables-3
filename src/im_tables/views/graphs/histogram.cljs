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
    (let [height-scale (linear-scale [0 (apply max (map :count points))] [0 50])]
      (into [:div.graph.histogram]
        (map-indexed (fn [idx d]
          [datum height-scale idx d (count points)])
              points)))))
