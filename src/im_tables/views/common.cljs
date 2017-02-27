(ns im-tables.views.common
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as reagent]))

(defn no-value [] [:span.no-value "[No value]"])
