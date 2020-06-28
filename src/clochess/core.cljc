(ns clochess.core
  (:require [clojure.test :refer [is]]
            [clochess.construct :refer :all]
            [clochess.debug :refer :all]))

(defn file-rank-to-string
  {:test (fn []
           (is (= (file-rank-to-string [0 0])
                  (file-rank-to-string 0 0)
                  "a1"))
           (is (= (file-rank-to-string [7 7])
                  (file-rank-to-string 7 7)
                  "h8"))
           (is (= (file-rank-to-string [3 4])
                  (file-rank-to-string 3 4)
                  "d5")))}
  ([coords]
   (apply file-rank-to-string coords))
  ([file rank]
   (str (char (+ 97 file)) (inc rank))))

(defn string-to-file-rank
  {:test (fn []
           (is (= (string-to-file-rank "a1")
                  [0 0]))
           (is (= (string-to-file-rank "h8")
                  [7 7]))
           (is (= (string-to-file-rank "d5")
                  [3 4])))}
  [s]
  (let [file (int (first s))
        rank (int (second s))]
    [(- file 97)
     (- rank 49)]))

(defn out-of-bounds?
  {:test (fn []
           (is (not-any? out-of-bounds? [[1 3] [6 2] [0 0] [7 7]]))
           (is (every? out-of-bounds? [[-1 4] [1 8] [9 9] [-3 -4]])))}
  ([[file rank]]
   (out-of-bounds? file rank))
  ([file rank]
   (not (and (<= 0 file 7)
             (<= 0 rank 7)))))

(defn free?
  {:test (fn []
           (is (-> (new-game)
                   (free? 4 4)))
           (is (not (-> (new-game)
                        (free? 0 0)))))}
  ([state [file rank]]
   (free? state file rank))
  ([state file rank]
   (= (get-piece state file rank) nil)))

(defn friendly?
  {:test (fn []
           (is (-> (new-game)
                   (friendly? 0 0 :white)))
           (is (-> (new-game)
                   (friendly? 7 7 :black)))
           (is (not (-> (new-game)
                        (friendly? 0 0 :black))))
           (is (not (-> (new-game)
                        (friendly? 7 7 :white)))))}
  ([state [file rank] color]
   (friendly? state file rank color))
  ([state file rank color]
  (let [piece (get-piece state file rank)]
    (= (:color piece) color))))

(def opposite-color
  {:white :black
   :black :white})

(defn enemy?
  {:test (fn []
           (is (-> (new-game)
                   (enemy? 0 0 :black)))
           (is (-> (new-game)
                   (enemy? 7 7 :white)))
           (is (not (-> (new-game)
                        (enemy? 0 0 :white))))
           (is (not (-> (new-game)
                        (enemy? 7 7 :black))))
           (is (not (-> (new-game)
                        (enemy? 4 4 :black)))))}
  [state file rank color]
  (let [piece (get-piece state file rank)]
    (= (:color piece) (color opposite-color))))

(defn capture-possible?
  {:test (fn []
           (is (-> (new-game)
                   (capture-possible? 0 0 :black)))
           (is (not (-> (new-game)
                        (capture-possible? 0 0 :white))))
           (is (not (-> (new-game)
                        (capture-possible? 4 4 :white)))))}
  [state file rank color]
  (and (not (nil? file))
       (not (nil? rank))
       (enemy? state file rank color)))

(defn remove-blocked
  {:test (fn []
           (is (= (-> (new-game)
                      (remove-blocked :white
                                      [[4 4] [4 5] [4 6] [4 7] [4 8]])))
               [[4 4] [4 5] [4 6] [4 7]])
           (is (= (-> (new-game)
                      (remove-blocked :black
                                      [[4 4] [4 5] [4 6] [4 7] [4 8]])))
               [[4 4] [4 5] [4 6]])
           (is (= (-> (new-game)
                      (remove-blocked :white
                                      [[4 4] [4 3] [4 2] [4 1] [4 0]])))
               [[4 4] [4 3] [4 2]])
           (is (= (-> (new-game)
                      (remove-blocked :black
                                      [[4 4] [4 3] [4 2] [4 1] [4 0]])))
               [[4 4] [4 3] [4 2] [4 1]]))}
  [state color coords]
  (let [[free not-free]   (split-with #(free? state %) coords)
        [file rank]       (first not-free)]
    (if (capture-possible? state file rank color)
      (conj free [file rank])
      free)))

(defn valid-moves-king
  [state file rank color]
  (remove #(friendly? state % color)
          '([(- file 1) (- rank 1)]
            [file       (- rank 1)]
            [(+ file 1) (- rank 1)]
            [(- file 1) rank]
            [(+ file 1) rank]
            [(- file 1) (+ rank 1)]
            [file       (+ rank 1)]
            [(+ file 1) (+ rank 1)])))

(defn valid-moves-queen
  [state file rank color]
  (println "Queen movement not yet implemented"))

(defn valid-moves-rook
  [state file rank color]
  (let [north (map vector (repeat file) (range (inc rank) 8))
        south (map vector (repeat file) (range (dec rank) -1 -1))
        east  (map vector (range (inc file) 8) (repeat rank))
        west  (map vector (range (dec file) -1 -1) (repeat rank))]
    (concat (remove-blocked state color north)
            (remove-blocked state color south)
            (remove-blocked state color east)
            (remove-blocked state color west))))

(defn valid-moves-bishop
  [state file rank color]
  (let [nw  (map vector (range (dec file) -1 -1) (range (inc rank) 8))
        sw  (map vector (range (dec file) -1 -1) (range (dec file) -1 -1))
        ne  (map vector (range (inc file) 8) (range (inc rank) 8 ))
        se  (map vector (range (inc file) 8) (range (dec file) -1 -1))]
    (concat (remove-blocked state color nw)
            (remove-blocked state color ne)
            (remove-blocked state color sw)
            (remove-blocked state color se))))

(defn valid-moves-queen
  [state file rank color]
  (concat (valid-moves-rook state file rank color)
          (valid-moves-bishop state file rank color)))

(defn valid-moves-knight
  [state file rank color]
  (println "Knight movement not yet implemented"))

(defn valid-moves-pawn
  [state file rank color]
  (println "Pawn movement not yet implemented"))

(defn valid-moves
  [state file rank]
  (let [piece      (get-piece state file rank)
        color      (:color piece)
        type       (:type piece)
        type-moves {:king   valid-moves-king
                    :queen  valid-moves-queen
                    :rook   valid-moves-rook
                    :bishop valid-moves-bishop
                    :knight valid-moves-knight
                    :pawn   valid-moves-pawn
                    nil     (fn [& _] '())}
        moves-fn   (get type-moves type)]
    (moves-fn state file rank color)))
