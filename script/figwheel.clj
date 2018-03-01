(require '[figwheel-sidecar.repl :as r]
         '[figwheel-sidecar.repl-api :as ra])

(ra/start-figwheel!
  {:figwheel-options
    {:css-dirs ["resources/public/game/css"]}
   :build-ids ["dev"]
   :all-builds
   [{:id "dev"
     :figwheel true
     :source-paths ["src" "test"]
     :compiler {:main 'cljs.zots.core
                :asset-path "js"
                :output-to "resources/public/game/js/main.js"
                :output-dir "resources/public/game/js"
                :verbose true
                :pretty-print true}}]})

(ra/cljs-repl)
