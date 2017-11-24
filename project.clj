(defproject zots "0.1.0"
  :description "Battle of Zots. It's your zots agains your enemy's"

  :source-paths ["src" "test"]
  :test-paths ["test"]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  ;:plugins [[lein-cljsbuild "1.1.7"]]

  :dependencies
   [[org.clojure/clojure "1.8.0"]
    [proto-repl "0.3.1"]
    [org.clojure/test.check "0.9.0"]
    [org.clojure/clojurescript "1.9.946"]
    [org.omcljs/om "1.0.0-beta1"]
    [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "test"]])
