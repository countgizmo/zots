(ns cljc.zots.game
  (:require [cljc.zots.specs :as specs]
            [clojure.spec.alpha :as s]
            [cljc.zots.board :as board]
            [cljc.zots.wall :as wall]))

(defn cell-available?
 "Checks if the cell is:
  - not occupied
  - not surrounded
  - not a wall."
 [cell]
 {:pre [(s/assert :specs/cell cell)]}
 (and
  (false? (:surrounded? cell))
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
  [game {:keys [x y turn] :as move}]
  {:pre [(s/assert :specs/game game)]}
  (and
   (s/valid? :specs/move move)
   (cell-available? (get-in game [:board [x y]]))
   (valid-turn? game turn)))

(defn random-turn
 "Generates a turn :blue or :red randomly."
 []
 {:post [(s/assert :specs/turn %)]}
 (rand-nth [:red :blue]))

(defn new-game
 "Generates new game state with random first turn (if none specified)."
 ([] (new-game nil))
 ([init-turn]
  {:post [(s/assert :specs/game %)]}
  {:board (board/gen-empty-board)
   :turn (if (nil? init-turn) (random-turn) init-turn)
   :score {:red 0 :blue 0}
   :walls {:red '() :blue '()}
   :slots #{}}))

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
  (:surrounded? cell)))

(defn calculate-score
 "Returns the number of surrounded enemy cells."
 [game player]
 {:pre [(s/assert :specs/game game) (s/assert :specs/turn player)]}
 (->> (:board game)
      (vals)
      (filter (partial cell-taken-by? player))
      (count)))

(defn update-score
 [game]
 (let [red (calculate-score game :red)
       blue (calculate-score game :blue)]
   (assoc-in game [:score] {:red red :blue blue})))

(defn take-cell
 "Marks the cell with player's tag if possible."
 [game {:keys [x y turn]}]
 {:pre [(s/assert :specs/game game) (s/assert :specs/turn turn)]}
 (if (cell-available? (get-in game [:board [x y]]))
  (assoc-in game [:board [x y] :player] turn)
  game))

(defn make-move
 "Returns next state of the game if possible to make a move.
  Otherwise returns the same state."
 [game {:keys [turn] :as move}]
 (-> (take-cell game move)
     (board/next-state)
     (assoc :turn (enemy turn))
     (update-score)
     (wall/walls-for-game)))

(defn enrich-from-db
  "Calculates additional information that was not stored into DB."
  [incomplete-game]
  (-> incomplete-game update-score wall/walls-for-game))

(defn cell-changed?
  [board-old [coord {:keys [surrounded? player status]}]]
  (let [{surrounded-old? :surrounded?
         player-old :player
         status-old :status}
        (get board-old coord)]
    (not
      (and (= surrounded? surrounded-old?)
           (= player player-old)
           (= status status-old)))))

(defn updated-cells-between-boards
  [board-old board-new]
  (into {}
    (filter (partial cell-changed? board-old) board-new)))
