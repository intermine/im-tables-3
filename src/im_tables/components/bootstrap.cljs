(ns im-tables.components.bootstrap
  (:require [reagent.core :as reagent]
            [reagent.dom.server :as server]
            [oops.core :refer [ocall oapply oget oset!]]))

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
     ;  (let [node (reagent/dom-node this)] (ocall (-> node js/$) "popover")))
     ;:component-will-unmount
     ;(fn [this]
     ;  (let [node (reagent/dom-node this)] (ocall (-> "popover" js/$) "remove")))
     ;:component-did-update
     ;(fn [this]
     ;  (.log js/console "me" (reagent/dom-node this))
     ;  (let [node (reagent/dom-node this)]
     ;    (ocall (-> "popover" js/$) "remove")
     ;    (ocall (-> node js/$) "popover")))
    :component-did-mount
    (fn [this])
       ;(.log js/console "d" (ocall (js/$ (reagent/dom-node this)) "popover"))

    :component-did-update
    (fn [this]
      (.log js/console "me" (js/$ (reagent/dom-node this)))
      (-> (js/$ (reagent/dom-node this))
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
        (let [bb (ocall (reagent/dom-node this) "getBoundingClientRect")]
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
