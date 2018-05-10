(defproject zots "0.1.9"
  :description "Battle of Zots. It's your zots agains your enemy's."

  :source-paths ["src"]
  :test-paths ["test"]
  :jvm-opts ["-Dclojure.spec.check-asserts=true" "-Xmx400m" "-server"]

  :plugins [[lein-cljsbuild "1.1.7"]]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/game/js"]
  :cljsbuild
   {:builds [{:id           "prod"
              :source-paths ["src/cljs"]
              :jar true
              :compiler     {:output-to "resources/public/game/js/main.js"
                             :optimizations   :advanced
                             :closure-defines {goog.DEBUG false}
                             :pretty-print    false
                             :elide-asserts true}}]}

  :profiles
  {:uberjar
   {:prep-tasks ["compile" ["cljsbuild" "once" "prod"]]
    :aot :all
    :main clj.zots.main
    :omit-source true
    :uberjar-name "zots.jar"}
   :dev
   {:dependencies
    [[proto-repl "0.3.1"]
     [org.clojure/test.check "0.10.0-alpha2"]
     [figwheel-sidecar "0.5.10" :scope "test"]
     [criterium "0.4.4"]]}}

 :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.10.238"]
   [org.omcljs/om "1.0.0-beta1"]
   [org.clojure/core.async "0.3.465"]
   [io.pedestal/pedestal.service "0.5.3"]
   [io.pedestal/pedestal.route   "0.5.3"]
   [io.pedestal/pedestal.jetty   "0.5.3"]
   [org.clojure/data.json        "0.2.6"]
   [org.slf4j/slf4j-simple       "1.7.21"]
   [clj-time "0.14.2"]
   [com.datomic/client-pro "0.8.14"]
   [integrant "0.6.3"]
   [com.walmartlabs/dyn-edn "0.1.0"]])
