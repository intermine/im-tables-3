(ns im-tables.components.bootstrap
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [reagent.dom.server :as server]
            [oops.core :refer [ocall oapply oget oset!]]
            [im-tables.utils :refer [place-below!]]))

(defn popover
  "Reagent wrapper for bootstrap's popover component. It accepts
  hiccup-style syntax in its :data-content attribute.
  Usage:
  [popover [:li {:data-trigger hover
                 :data-placement right
                 :data-content [:div [:h1 Hello] [:h4 Goodbye]]} Hover-Over-Me]]"
  []
  (reagent/create-class
   {;:component-did-mount
     ;(fn [this]
     ;  (let [node (dom/dom-node this)] (ocall (-> node js/$) "popover")))
     ;:component-will-unmount
     ;(fn [this]
     ;  (let [node (dom/dom-node this)] (ocall (-> "popover" js/$) "remove")))
     ;:component-did-update
     ;(fn [this]
     ;  (.log js/console "me" (dom/dom-node this))
     ;  (let [node (dom/dom-node this)]
     ;    (ocall (-> "popover" js/$) "remove")
     ;    (ocall (-> node js/$) "popover")))
    :component-did-mount
    (fn [this])
       ;(.log js/console "d" (ocall (js/$ (dom/dom-node this)) "popover"))

    :component-did-update
    (fn [this]
      (.log js/console "me" (js/$ (dom/dom-node this)))
      (-> (js/$ (dom/dom-node this))
           ;(ocall "popover" "destroy")
          (ocall "popover" "toggle")))
    :reagent-render
    (fn [[element attributes & rest]]
      [element (-> attributes
                   (assoc :data-html true)
                   (assoc :data-container "body")
                   (update :data-content server/render-to-static-markup)) rest])}))

(defn inner-tooltip []
  (fn [parent-data show-atom content]
    (reagent/create-class
     {:name "ARROWBOX"
      :reagent-render
      [:div.arrow_box
       {:on-mouse-enter (fn [] (reset! show-atom false))
        :style          {:position "absolute"
                         :top      (:height parent-data)}}
       content]})))

(defn tooltip
  "Reagent wrapper for bootstrap's popover component. It accepts
  hiccup-style syntax in its :data-content attribute.
  Usage:
  [popover [:li {:data-trigger hover
                 :data-placement right
                 :data-content [:div [:h1 Hello] [:h4 Goodbye]]} Hover-Over-Me]]"
  []
  (let [mystate (reagent/atom {})
        show?   (reagent/atom false)]
    (reagent/create-class
     {:name "Tooltip"
      :component-did-mount
      (fn [this]
        (let [bb (ocall (dom/dom-node this) "getBoundingClientRect")]
          (swap! mystate assoc
                 :width (oget bb "width")
                 :height (oget bb "height")
                 :left (oget bb "left")
                 :right (oget bb "right")
                 :top (oget bb "top")
                 :bottom (oget bb "bottom"))))
      :component-did-update
      (fn [this])
      :reagent-render
      (fn [[element attributes & rest]]
        [element (-> attributes
                     (assoc :on-mouse-enter (fn []
                                              (do
                                                (if-let [f (:on-mouse-enter attributes)] (f))
                                                (reset! show? true))))
                     (assoc :on-mouse-leave (fn [x]
                                              (do
                                                (if-let [f (:on-mouse-leave attributes)] (f))
                                                (reset! show? false)))))
         (if @show?
           [:div.test
            [:div.arrow_box
             {:on-mouse-enter (fn [] (reset! show? false))
              :style          {:position "absolute"
                               :top      (:height @mystate)}}
             (:data-content attributes)]])

                  ;[inner-tooltip @mystate show? (:data-content attributes)]

         rest])})))

(defn modal []
  (fn [{:keys [header body footer]}]
    (js/console.log "FOOTER" footer)
    [:div#testModal.modal.fade {:role "dialog"}
     [:div.modal-dialog
      [:div.modal-content
       [:div.modal-header header]
       body
       [:div.modal-footer
        footer]]]]))

(defn dropdown []
  (let [*open? (reagent/atom false)
        !container (atom nil)
        !dropdown (atom nil)
        handle-click (fn [evt]
                       (when-let [container @!container]
                         (when-not (ocall container :contains (oget evt :target))
                           (reset! *open? false))))]
    (reagent/create-class
     {:component-did-mount
      (fn [_]
        (.addEventListener js/document "mousedown" handle-click))
      :component-will-unmount
      (fn [_]
        (.removeEventListener js/document "mousedown" handle-click)
        (remove-watch *open? :on-close))
      :reagent-render
      (fn [{:keys [button toggle-ref on-close right?]
            :or {on-close #()}} & children]
        (add-watch *open? :on-close #(when (= [%3 %4] [true false]) (on-close)))
        [:span.dropdown
         {:class (when @*open? :open)
          :aria-expanded @*open?
          :ref #(reset! !container %)}
         [button {:on-click (fn [_evt]
                              (reset! *open? true)
                              (when-let [toggle @toggle-ref]
                                (when-let [dropdown @!dropdown]
                                  (place-below! dropdown toggle
                                                :right? right?))))}]
         (into [:div.dropdown-menu
                {:ref #(reset! !dropdown %)}]
               (when @*open?
                 children))])})))

#_:clj-kondo/ignore
(comment
  "Example of the dropdown component being used in a complex nested layout.
  This is what it was originally made for, but it turns out just reusing the
  toolbar component nested in the outer join table worked better."
  [dropdown
   {:button (fn [{:keys [on-click]}]
              [:i.fa.fa-bar-chart.dropdown-toggle
               {:title (str "Summarise " view " columns")
                :on-click on-click
                :ref #(reset! !toggle %)}])
    :toggle-ref !toggle
    :right? right?}
   (into [:ul]
         (for [subview @joined-views]
           [dropdown
            {:button (fn [{:keys [on-click]}]
                       [:li {:on-click (fn [evt]
                                         (reset! summarize-view subview)
                                         (dispatch [:main/summarize-column loc @summarize-view])
                                         (on-click evt))}
                        subview])
             :on-close #(dispatch [:select/clear-selection loc @summarize-view])
             :right? right?}
            [column-summary loc @summarize-view]]))])
