(ns clj.zots.debug-printer)

(def player->symbol
 {:red "X" :blue "O" :blue-fill "+" :none "_"})

(defn row->str [r]
 (reduce #(str %1 (-> %2 :player player->symbol)) "" r))

(defn board->row [b]
 (reduce #(str %1 (row->str %2) "\n") "" b))

(defn print-board [state]
 (print (board->row (get-in state [:board]))))
