(ns prone.ui.components.app
  (:require [cljs.core.async :refer [put! map>]]
            [prone.ui.components.map-browser :refer [MapBrowser]]
            [prone.ui.components.stack-frame :refer [StackFrame]]
            [prone.ui.components.code-excerpt :refer [CodeExcerpt]]
            [prone.ui.utils :refer [action]]
            [quiescent :as q :include-macros true]
            [quiescent.dom :as d]))

(q/defcomponent ErrorHeader
  [{:keys [error request paths]} chans]
  (d/header {:className "exception"}
            (d/h2 {}
                  (d/strong {} (:type error))
                  (d/span {} " at " (:uri request))
                  (when (or (:caused-by error) (seq (:error paths)))
                    (d/span {:className "caused-by"}
                            (when (seq (:error paths))
                              (d/a {:href "#"
                                    :onClick (action #(put! (:navigate-data chans)
                                                            [:error [:reset (butlast (:error paths))]]))}
                                   "< back"))
                            (when-let [caused-by (:caused-by error)]
                              (d/span {} " Caused by " (d/a {:href "#"
                                                             :onClick (action #(put! (:navigate-data chans)
                                                                                     [:error [:concat [:caused-by]]]))}
                                                            (:type caused-by)))))))
            (d/p {} (or (:message error)
                        (d/span {} (:class-name error)
                                (d/span {:className "subtle"} " [no message]"))))))

(q/defcomponent DebugHeader
  [{:keys [request]} chans]
  (d/header {:className "exception"}
            (d/h2 {}
                  (d/span {} "Tired of seeing this page? Remove calls to "
                          "prone.debug/debug - and stop causing exceptions"))
            (d/p {} "Halted for debugging")))

(q/defcomponent Header
  [data chans]
  (if (:error data)
    (ErrorHeader data chans)
    (DebugHeader data chans)))

(q/defcomponent StackFrameLink
  [{:keys [frame-selection target name]} chans]
  (d/a {:href "#"
        :className (when (= target frame-selection) "selected")
        :onClick (action #(put! (:change-frame-selection chans) target))}
       name))

(q/defcomponent Sidebar
  [{:keys [error frame-selection selected-src-loc debug-data active-frames]} chans]
  (d/nav {:className "sidebar"}
         (d/nav {:className "tabs"}
                (when error
                  (StackFrameLink {:frame-selection frame-selection
                                   :target :application
                                   :name "Application Frames"} chans))
                (when error
                  (StackFrameLink {:frame-selection frame-selection
                                   :target :all
                                   :name "All Frames"} chans))
                (when (seq debug-data)
                  (StackFrameLink {:frame-selection frame-selection
                                   :target :debug
                                   :name "Debug Calls"} chans)))
         (apply d/ul {:className "frames" :id "frames"}
                (map #(StackFrame {:frame %
                                   :selected? (= % selected-src-loc)}
                                  (:select-src-loc chans))
                     active-frames))))

(q/defcomponent Body
  [{:keys [frame-selection selected-src-loc debug-data error paths browsables] :as data} {:keys [navigate-data]}]
  (let [debugging? (= :debug frame-selection)
        local-browsables (:browsables (if debugging? selected-src-loc error))
        heading (when (= :debug frame-selection) (:message debug-data))]
    (apply d/div {:className "frame_info" :id "frame-info"}
           (CodeExcerpt selected-src-loc)
           (when heading (d/h2 {:className "sub-heading"} heading))
           (map #(d/div {:className "sub"}
                        (MapBrowser {:data (:data %)
                                     :path (get paths %)
                                     :name (:name %)}
                                    (map> (fn [v] [% v]) navigate-data)))
                (concat local-browsables browsables)))))

(q/defcomponent ProneUI
  "Prone's main UI component - the page's frame"
  [data chans]
  (d/div {:className "top"}
         (Header data chans)
         (d/section {:className "backtrace"}
                    (Sidebar data chans)
                    (Body data chans))))
