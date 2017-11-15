(defproject zots "0.1.0"
  :description "Battle of Zots. It's your zots agains your enemy's"

  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :dependencies
   [[org.clojure/clojure "1.8.0"]
    [proto-repl "0.3.1"]
    [org.clojure/test.check "0.9.0"]])
