(ns clj.zots.debug-printer)

(def player->symbol
 {:red "R" :blue "B" :none "_"})

(def status->symbol
 {:wall "W" :active "A"})

(defn get-symbol
 [cell]
 (if (= :active (:status cell))
   (-> cell :player player->symbol)
   (if (= :wall (:status cell))
     "W")))

(defn row->str [r]
 (reduce #(str %1 (get-symbol %2)) "" r))

(defn board->row [b]
 (reduce #(str %1 (row->str %2) "\n") "" b))

(defn print-board [state]
 (print (board->row (reverse (get-in state [:board])))))
