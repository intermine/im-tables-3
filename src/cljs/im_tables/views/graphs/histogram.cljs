(ns im-tables.views.graphs.histogram)


(defn linear-scale [[r1-lower r1-upper] [r2-lower r2-upper]]
  (fn [v]
    (+ (/ (* (- v r1-lower) (- r2-upper r2-lower))
          (- r1-upper r1-lower))
       r2-lower)))

(defn datum []
  (fn [height-scale idx {:keys [item count]}]
    [:rect.histo {:x      idx
            :y      (- 60 (height-scale count))
            :width  1
            :height (height-scale count)}]))

(defn main []
  (fn [points]
    (let [height-scale (linear-scale [0 (apply max (map :count points))] [0 60])]
      [:svg.graph
       {:width                 "100%"
        :height                "100px"
        :preserve-aspect-ratio "none"
        :shapeRendering "crispEdges"
        :view-box              (str "0 0 " (count points) " 60" )}
       (into [:g]
             (map-indexed (fn [idx d]
                            [datum height-scale idx d (count points)])
                          points))])))