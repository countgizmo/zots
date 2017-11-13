(ns test.clj.zots.board-test
 (:require [clojure.test :refer :all])
 (:require [clj.zots.board :as board])
 (:require [clj.zots.debug-printer :as dp]))

(def simple-surround
 [[{:y 0, :surrounded false, :status :active, :player :none, :x 0}
   {:y 0, :surrounded false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 1, :surrounded false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded false, :status :active, :player :blue, :x 1}
   {:y 1, :surrounded false, :status :active, :player :red, :x 2}]
  [{:y 2, :surrounded false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded false, :status :active, :player :red, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 2}]])

(def not-so-simple-surround
 [[{:y 0, :surrounded false, :status :active, :player :red, :x 0}
   {:y 0, :surrounded false, :status :active, :player :red, :x 1}
   {:y 0, :surrounded false, :status :active, :player :none, :x 2}
   {:y 0, :surrounded false, :status :active, :player :none, :x 3}]
  [{:y 1, :surrounded false, :status :active, :player :red, :x 0}
   {:y 1, :surrounded false, :status :active, :player :blue, :x 1}
   {:y 1, :surrounded false, :status :active, :player :red, :x 2}
   {:y 1, :surrounded false, :status :active, :player :none, :x 3}]
  [{:y 2, :surrounded false, :status :active, :player :red, :x 0}
   {:y 2, :surrounded false, :status :active, :player :none, :x 1}
   {:y 2, :surrounded false, :status :active, :player :red, :x 2}
   {:y 2, :surrounded false, :status :active, :player :none, :x 3}]
  [{:y 3, :surrounded false, :status :active, :player :none, :x 0}
   {:y 3, :surrounded false, :status :active, :player :red, :x 1}
   {:y 3, :surrounded false, :status :active, :player :none, :x 2}
   {:y 3, :surrounded false, :status :active, :player :none, :x 3}]])

(def already-surrounded
 (-> not-so-simple-surround
     (assoc-in [1 1 :surrounded] true)
     (assoc-in [0 1 :status] :wall)
     (assoc-in [0 0 :status] :wall)
     (assoc-in [1 0 :status] :wall)
     (assoc-in [1 2 :status] :wall)
     (assoc-in [2 0 :status] :wall)
     (assoc-in [2 1 :surrounded] true)
     (assoc-in [2 2 :status] :wall)
     (assoc-in [3 1 :status] :wall)))

(defn make-state
 ([b] (make-state b []))
 ([b t]
  {:board b
   :target t
   :visited []}))

(defn wall?
 [c]
 (= :wall (:status c)))

(def visited [[1 0] [2 3]])

(deftest test-visited
 (is (true? (board/visited? 1 0 visited))))

(deftest test-visited-false
 (is (false? (board/visited? 1 1 visited))))

(deftest test-visited-empty
 (is (false? (board/visited? 1 0 []))))

(deftest test-add-visited
 (is (= [[1 0]] (board/add-visited 1 0 []))))

(deftest test-add-visited-twice
 (is (= [[2 2]] (board/add-visited 2 2 [[2 2]]))))

(deftest test-should-visit-below-x
 (is (false? (board/should-visit? -1 0 simple-surround))))

(deftest test-should-visit-below-y
 (is (false? (board/should-visit? 0 -1 simple-surround))))

(deftest test-should-visit-below-xy
 (is (false? (board/should-visit? -1 -1 simple-surround))))

(deftest test-should-visit-below-above-x
 (is (false? (board/should-visit? 3 0 simple-surround))))

(deftest test-should-visit-below-above-y
 (is (false? (board/should-visit? 0 3 simple-surround))))

(deftest test-should-visit-below-above-xy
 (is (false? (board/should-visit? 3 3 simple-surround))))

(deftest test-should-visit-true-1
 (is (true? (board/should-visit? 2 2 simple-surround))))

(deftest test-should-visit-true-2
 (is (true? (board/should-visit? 0 0 simple-surround))))

(deftest test-should-visit-true-3
 (is (true? (board/should-visit? 1 1 simple-surround))))

(deftest test-should-visit-true-4
 (is (true? (board/should-visit? 2 0 simple-surround))))

(deftest test-get-target-player-blue
 (let [state (make-state simple-surround [1 1])]
   (is (= :blue (board/get-target-player state)))))

(deftest test-get-target-player-red
 (let [state (make-state simple-surround [1 2])]
   (is (= :red (board/get-target-player state)))))

(deftest test-get-target-player-none
 (let [state (make-state simple-surround [0 0])]
   (is (= :none (board/get-target-player state)))))

(deftest test-get-target-player-nil
 (let [state (make-state simple-surround [-1 3])]
   (is (nil? (board/get-target-player state)))))

(deftest test-can-fill
 (let [red-cell {:player :red :status :active :surrounded false :x 0 :y 0}
       blue-cell (assoc red-cell :player :blue)
       none-cell (assoc red-cell :player :none)]
   (is (true? (board/can-fill? red-cell :red)))
   (is (true? (board/can-fill? none-cell :red)))
   (is (false? (board/can-fill? blue-cell :red)))
   (is (false? (board/can-fill? {} :red)))))

(deftest test-flood-cell
 (let [state (make-state not-so-simple-surround [2 1])]
  (is (true? (board/flood-cell? 2 1 state)))
  (is (true? (board/flood-cell? 3 1 state)))
  (is (true? (board/flood-cell? 2 2 state)))
  (is (false? (board/flood-cell? 1 1 state)))
  (is (false? (board/flood-cell? -1 -1 state)))))

(deftest test-touch-border
 (is (true? (board/touch-border? [0 0] not-so-simple-surround)))
 (is (true? (board/touch-border? [0 2] not-so-simple-surround)))
 (is (true? (board/touch-border? [1 0] not-so-simple-surround)))
 (is (true? (board/touch-border? [3 3] not-so-simple-surround)))
 (is (false? (board/touch-border? [1 1] not-so-simple-surround)))
 (is (false? (board/touch-border? [2 2] not-so-simple-surround)))
 (is (false? (board/touch-border? [2 1] not-so-simple-surround))))

(deftest test-reach-border
 (is (true? (board/reach-border? [[2 1] [3 1]] not-so-simple-surround)))
 (is (true? (board/reach-border? [[2 1] [3 1] [2 0]] not-so-simple-surround)))
 (is (true? (board/reach-border? [[0 0] [0 1] [1 1]] not-so-simple-surround)))
 (is (false? (board/reach-border? [] not-so-simple-surround)))
 (is (false? (board/reach-border? [[2 1]] not-so-simple-surround)))
 (is (false? (board/reach-border? [[2 1] [2 2] [1 1] [1 2]] not-so-simple-surround))))

(deftest test-update-trail
 (let [state (make-state not-so-simple-surround [2 1])]
  (is (nil? (:trail (board/update-trail 1 1 state))))
  (is (= [[2 0]] (:trail (board/update-trail 2 0 state))))
  (is (= [[2 1] [2 0]]
        (:trail (->> (board/update-trail 2 1 state)
                     (board/update-trail 2 0)))))
  (is (= [[2 1]]
        (:trail (->> (board/update-trail 2 1 state)
                     (board/update-trail 1 1)))))))

(def cell {:x 1 :y 2 :player :red :surrounded false :status :active})
(board/can-fill? cell :red)

(deftest test-fill-flood-1
 (let [state (make-state not-so-simple-surround [1 1])
       expected [[1 1] [1 2]]
       new-state (board/fill-flood 1 1 state)]
  (is (and
        (= expected (:trail new-state))
        (= (:board state) (:board new-state))))))

(deftest test-fill-flood-2
 (let [state (make-state not-so-simple-surround [2 1])
       expected [[2 1] [2 2] [2 3] [3 3] [3 2]
                 [3 1] [3 0] [2 0] [1 0] [0 0]
                 [0 1] [0 2] [0 3] [1 3] [1 2]]
       new-state (board/fill-flood 2 1 state)]
  (is (= expected (:trail new-state)))
  (is (= (:board state) (:board new-state)))))

(deftest test-mark-surrounded-empty-trail
 (let [state (make-state not-so-simple-surround [2 1])
       new-state (board/mark-surrounded [2 1] state)
       board (:board new-state)
       fb (flatten board)]
   (is (false? (get-in board [1 2 :surrounded])))
   (is (zero? (count (filter #(:surrounded %) fb))))))

(deftest test-mark-surrounded-false-non-empty-trail
 (testing "A cell that has a trail that touches border."
   (let [state (make-state not-so-simple-surround [2 1])
         state (assoc state :trail
                            [[2 1] [2 2] [2 3] [3 3] [3 2]
                             [3 1] [3 0] [2 0] [1 0] [0 0]
                             [0 1] [0 2] [0 3] [1 3] [1 2]])
         new-state (board/mark-surrounded [2 1] state)
         board (:board new-state)
         fb (flatten board)]
     (is (false? (get-in board [1 2 :surrounded])))
     (is (zero? (count (filter #(:surrounded %) fb)))))))

(deftest test-mark-surrounded-when-already-surrounded
 (testing "A cell that has a trail that touches border in a board with
           already surrounded cell."
   (let [state (make-state already-surrounded [2 1])
         state (assoc state :trail
                            [[2 1] [2 2] [2 3] [3 3] [3 2]
                             [3 1] [3 0] [2 0] [1 0] [0 0]
                             [0 1] [0 2] [0 3] [1 3] [1 2]])
         new-state (board/mark-surrounded [2 1] state)
         board (:board new-state)
         fb (flatten board)]
     (is (false? (get-in board [1 2 :surrounded])))
     (is (= 2 (count (filter #(:surrounded %) fb)))))))

(deftest test-mark-surrounded-valid-surround
 (testing "Two cells surrounded by reds should be marked."
   (let [state (make-state not-so-simple-surround [1 1])
         state (assoc state :trail [[1 1] [1 2]])
         new-state (board/mark-surrounded [1 1] state)
         board (:board new-state)
         fb (flatten board)]
     (is (true? (get-in board [1 1 :surrounded])))
     (is (= 1 (count (filter #(:surrounded %) fb)))))))

(deftest test-mark-as-wall
 (let [cell {:x 1 :y 2 :status :active :surrounded false :player :red}
       empty-cell (assoc cell :player :none)
       wall-cell (assoc cell :status :wall)
       friendly-cell (assoc cell :player :blue)]
   (is (= :wall (board/mark-as-wall cell :blue)))
   (is (= :active (board/mark-as-wall empty-cell :blue)))
   (is (= :wall (board/mark-as-wall wall-cell :blue)))
   (is (= :active (board/mark-as-wall friendly-cell :blue)))))

(deftest test-collect-cells-arround
 (let [state (make-state not-so-simple-surround [2 1])
       corner-cells (board/collect-cells-around [3 3] state)
       mid-cells (board/collect-cells-around [1 1] state)]
   (is (= 3 (count corner-cells)))
   (is (= 8 (count mid-cells)))))

(deftest test-mark-wall-around-cell-1
 (let [state (make-state not-so-simple-surround [2 1])
       state (assoc state :trail
                            [[2 1] [2 2] [2 3] [3 3] [3 2]
                             [3 1] [3 0] [2 0] [1 0] [0 0]
                             [0 1] [0 2] [0 3] [1 3] [1 2]])
       new-state (board/mark-surrounded [2 0] state)
       new-state (board/mark-wall-around-cell [2 0] state)
       board (:board new-state)
       fb (flatten board)]
   (is (zero? (count (filter #(= :wall (:status %)) fb))))))

(deftest test-mark-wall-around-cell-2
 (let [state (make-state not-so-simple-surround [1 1])
       state (assoc state :trail [[1 1] [1 2]])
       new-state (board/mark-surrounded [1 1] state)
       new-state (board/mark-wall-around-cell [1 1] new-state)
       board (:board new-state)
       fb (flatten board)]
   (is (= 6 (count (filter #(= :wall (:status %)) fb))))))

(deftest test-mark-walls-around-trail-surrounded
  (let [state (make-state not-so-simple-surround [1 1])
        state (assoc state :trail [[1 1] [1 2]])
        state (board/mark-walls-around-trail state)
        board (:board state)
        fb (flatten board)]
   (is (= 7 (count (filter #(= :wall (:status %)) fb))))))

(deftest test-mark-wall-around-trail-not-surrounded
  (let [state (make-state not-so-simple-surround [2 1])
          state (assoc state :trail
                             [[2 1] [2 2] [2 3] [3 3] [3 2]
                              [3 1] [3 0] [2 0] [1 0] [0 0]
                              [0 1] [0 2] [0 3] [1 3] [1 2]])
          state (board/mark-walls-around-trail state)
          board (:board state)
          fb (flatten board)]
     (is (zero? (count (filter #(= :wall (:status %)) fb))))))

(deftest surround-state-detection
 (testing "A simple situation when a cell is surrounded by enemy"
   (let [next-state (board/next-state (make-state simple-surround))
         board (:board next-state)
         fb (flatten board)]
     (is (true? (get-in board [1 1 :surrounded])))
     (is (= 1 (count (filter #(:surrounded %) fb))))
     (is (= 7 (count (filter wall? fb)))))))

(deftest not-so-simple-surround-detection
 (testing "A more complex surround situation"
   (let [next-state (board/next-state (make-state not-so-simple-surround))
         board (:board next-state)
         fb (flatten board)]
     (is (true? (get-in board [1 1 :surrounded])))
     (is (= 2 (count (filter #(:surrounded %) fb))))
     (is (= 7 (count (filter wall? fb)))))))
