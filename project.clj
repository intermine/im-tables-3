(defproject org.intermine/im-tables "0.7.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671"]
                 [reagent "0.7.0" :exclusions [cljsjs/react]]
                 [cljsjs/react-with-addons "15.6.1-0"]
                 [re-frame "0.10.2"]
                 [compojure "1.6.0"]
                 [ring "1.6.3"]
                 [cljs-http "0.1.44"]
                 [day8.re-frame/async-flow-fx "0.0.8"]
                 [joshkh/ctrlz "0.3.0"]
                 [binaryage/oops "0.5.6"]
                 [inflections "0.13.0"]
                 [re-frisk "0.5.2"]
                 [org.clojure/core.async "0.3.443"]
                 [criterium "0.4.4"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 [intermine/imcljs "0.1.36"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-less "1.7.5"]
            [lein-ancient "0.6.10"]]

  :repositories {"clojars" {:sign-releases false}}

  :min-lein-version "2.5.3"

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 3448}

  :less {:source-paths ["less"]
         :target-path "resources/public/css"}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.7"]]

    :plugins [[lein-figwheel "0.5.7"]
              [lein-doo "0.1.7"]]
    }}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :figwheel {:on-jsload "im-tables.core/mount-root"}
     :compiler {:main im-tables.core
                :output-to "resources/public/js/compiled/app.js"
                :output-dir "resources/public/js/compiled/out"
                :asset-path "js/compiled/out"
                :source-map-timestamp true
                :preloads [devtools.preload]
                :parallel-build true
                :external-config {:devtools/config {:features-to-install :all}}
                }}

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
                :optimizations :none}}
    ]}

  ;:prep-tasks [["cljsbuild" "once" "min"] ["less" "once"] "compile"]
  )
