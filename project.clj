(defproject zots "0.1.0"
  :description "Battle of Zots. It's your zots agains your enemy's"

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]
                 [ring "1.4.0"]
                 [compojure "1.4.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [om-sync "0.1.1"]
                 [com.datomic/datomic-free "0.9.5130" :exclusions [joda-time]]]

  :plugins [[lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.0"]]


  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]
  :test-paths ["test" "src/test/clj"]
  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/main.js"]

  :figwheel {:ring-handler zots.core/handler}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/clj" "src/cljs"]
                        :figwheel true
                        :compiler {:output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js/out"
                                   :main zots.core
                                   :asset-path "js/out"
                                   :optimizations :none
                                   :source-map true}}]})
