(defproject zots "0.1.0"
  :description "Battle of Zots. It's your zots agains your enemy's"

  :source-paths ["src"]
  :test-paths ["test"]
  :jvm-opts ["-Dclojure.spec.check-asserts=true" "-Xmx1g" "-server"]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :dependencies
   [[org.clojure/clojure "1.9.0"]
    [proto-repl "0.3.1"]
    [org.clojure/test.check "0.10.0-alpha2"]
    [org.clojure/clojurescript "1.9.946"]
    [org.omcljs/om "1.0.0-beta1"]
    [org.clojure/core.async "0.3.465"]
    [figwheel-sidecar "0.5.10" :scope "test"]])
