(ns zots.core-test
	(:use clojure.test
				zots.core))

(def expected-cell
	{:col 0 :row 0 :player nil :active true})


(deftest get-cell
	(testing "Should be able to produce a cell"
		(is (= expected-cell (gen-cell 0 0)))))