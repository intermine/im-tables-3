(defproject org.intermine/im-tables "0.8.3"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/core.async "0.4.500"]
                 [re-frame "0.10.8"]
                 [day8.re-frame/async-flow-fx "0.1.0"]
                 [day8.re-frame/forward-events-fx "0.0.6"]
                 [reagent "0.8.1"]
                 [cljsjs/react-transition-group "1.2.0-0"
                  :exclusions [cljsjs/react cljsjs/react-dom]]
                 [cljsjs/highlight "9.12.0-2"]
                 [compojure "1.6.1"]
                 [ring "1.7.1"]
                 [cljs-http "0.1.46"]
                 [joshkh/ctrlz "0.3.0"]
                 [binaryage/oops "0.7.0"]
                 [inflections "0.13.2"]
                 [criterium "0.4.5"]
                 [org.intermine/imcljs "1.0.1"]
                 [day8.re-frame/test "0.1.5"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.15"]
            [lein-pdo "0.1.1"]
            [lein-cljfmt "0.6.1"]]

  :aliases {"dev" ["do" "clean"
                   ["pdo"
                    ["trampoline" "less" "auto"]
                    ["with-profile" "+repl" "run"]]]
            "build" ["do" "clean"
                     ["less" "once"]
                     ["cljsbuild" "once" "min"]]
            "deploy" ["with-profile" "+uberjar" "deploy" "clojars"]
            "format" ["cljfmt" "fix"]
            "kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]}

  :repositories {"clojars" {:sign-releases false}}

  :min-lein-version "2.8.1"

  :source-paths ["src"]

  :test-paths ["test/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 3448}

  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]
                 :timeout 120000}

  :less {:source-paths ["less"]
         :target-path "resources/public/css"}

  :main im-tables.core

  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.10"]
                                  [day8.re-frame/re-frame-10x "0.4.4"]
                                  [day8.re-frame/tracing "0.5.1"]
                                  [figwheel-sidecar "0.5.19"]
                                  [cider/piggieback "0.4.1"]]
                   :plugins [[lein-figwheel "0.5.19"]]}
             :repl {:source-paths ["dev"]}
             :uberjar {:prep-tasks ["build"]}
             :kaocha {:dependencies [[lambdaisland/kaocha "0.0-554"]
                                     [lambdaisland/kaocha-cljs "0.0-59"]]}}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel {:on-jsload "im-tables.core/mount-root"}
                        :compiler {:main im-tables.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true
                                   :parallel-build true
                                   :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true
                                                     "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                                   :preloads [devtools.preload
                                              day8.re-frame-10x.preload]
                                   :external-config {:devtools/config {:features-to-install :all}}}}


                       {:id "min"
                        :source-paths ["src"]
                        :jar true
                        :compiler {:main im-tables.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}

                       {:id "test"
                        :source-paths ["src" "test/cljs"]
                        :compiler {:main im-tables.runner
                                   :output-to "resources/public/js/compiled/test.js"
                                   :output-dir "resources/public/js/compiled/test/out"
                                   :optimizations :none}}]})
