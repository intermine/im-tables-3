(ns im-tables.views.dashboard.undo
  (:require [re-frame.core :refer [dispatch subscribe]]))

(defn main [loc]
  (let [undos (subscribe [:undos? loc])
        redos (subscribe [:redos? loc])
        explanations (subscribe [:undo-explanations loc])
        redo-explanations (subscribe [:redo-explanations loc])]
    (fn []
      [:div.btn-toolbar
       [:div.btn-group
        [:button.btn.btn-default
         {:disabled (not @undos)
          :on-click (fn []
                      (dispatch [:undo loc])
                      ;(dispatch [:purge-redos loc])

                      )}
         [:i.fa.fa-undo] " Undo"]
        [:button.btn.btn-default.dropdown-toggle {:data-toggle "dropdown"
                                                  :disabled (not @undos)} [:span.caret]]
        (into [:ul.dropdown-menu.list-group]
              (map-indexed (fn [idx explanation]
                             [:li.list-group-item
                              {:on-click (fn []
                                           (dispatch [:undo loc (inc idx)]))} explanation]))
              (reverse @explanations))]
       (when @redos
         [:div.btn-group
          [:button.btn.btn-default
           {
            :disabled (not @redos)
            :on-click (fn []
                        (dispatch [:redo loc])
                        ;(dispatch [:purge-redos loc])
                        )}
           [:i.fa.fa-repeat] " Redo"]])])))