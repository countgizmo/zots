(ns cljc.zots.game
  (:require [cljc.zots.specs :as specs]
            [clojure.spec.alpha :as s]
            [cljc.zots.board :as board]))

(defn cell-available?
 "Checks if the cell is:
  - not occupied
  - not surrounded
  - not a wall."
 [cell]
 {:pre [(s/assert :specs/cell cell)]}
 (and
  (false? (:surrounded cell))
  (= (:player cell) :none)
  (= (:status cell) :active)))

(defn valid-turn?
 "Checks if the turn is the expected one for the game."
 [game turn]
 {:pre [(s/assert :specs/game game)]}
 (= (:turn game) turn))

(defn valid-move?
 "Checks if player's move is valid.
  Rules:
  1. Move should contain x, y and player slots.
  2. Move should target free not surrounded cell.
  3. Move should constaint to the sequence of turns."
  [game move]
  {:pre [(s/assert :specs/game game)]}
  (and
   (s/valid? :specs/move move)
   (cell-available? (get-in game [:board (:y move) (:x move)]))
   (valid-turn? game (:turn move))))

(defn random-turn
 "Generates a turn :blue or :red randomly."
 []
 {:post [(s/assert :specs/turn %)]}
 (rand-nth [:red :blue]))

(defn new-game
 "Generates new game state with random first turn."
 []
 {:post [(s/assert :specs/game %)]}
 {:board (board/gen-empty-board)
  :turn (random-turn)
  :score {:red 0 :blue 0}
  :walls {:red '() :blue '()}})

(defn enemy
 "Returns enemy symbol for speicfied player."
 [player]
 {:pre [(s/assert :specs/turn player)]}
 (if (= player :red) :blue :red))

(defn cell-taken-by?
 "Checks if the cell is taken by the player.
  Means the cell belongs to the enemy and is surrounded."
 [player cell]
 (and
  (= (:player cell) (enemy player))
  (:surrounded cell)))

(defn calculate-score
 "Returns the number of surrounded enemy cells."
 [game player]
 {:pre [(s/assert :specs/game game) (s/assert :specs/turn player)]}
 (->> (flatten (:board game))
     (filter (partial cell-taken-by? player))
     (count)))

(defn take-cell
 "Marks the cell with player's tag if possible."
 [game {:keys [x y turn]}]
 {:pre [(s/assert :specs/game game) (s/assert :specs/turn turn)]}
 (if (cell-available? (board/get-cell (:board game) x y))
  (assoc-in game [:board y x :player] turn)
  game))

(defn make-move
 "Returns next state of the game if possible to make a move.
  Otherwise returns the same state."
 [game {:keys [x y turn] :as move}]
 (let [next-game (take-cell game move)]
   (-> (update-in next-game [:board] board/next-state)
       (assoc :turn (enemy turn)))))
