
; If everything is OK, you should be able to copy-paste the following lines without error.

(require '[clojure.core.async :refer [chan >! <! >!! <!! onto-chan] :as a])
(require '[clojure.test :refer :all])


; channel is like a fifo queue with a buffer
(def buffer-size 1)
(def ch1 (chan buffer-size))

; one can write to it
(>!! ch1 5)
; and read from it
(def read (<!! ch1))
(is (= read 5))

; Exercise 1: try in REPL: (<!! ch) what happens?

; channels are fun because you can use map etc.
(def ch-int (chan 10))
(>!! ch-int 40)
(def ch-str (a/map str [ch-int]))
(is (= (<!! ch-str) "40"))

;; Exersice 2: write a
;(def ch-ints (chan 10))
;(a/onto-chan ch-ints (range 1 10))
;(def ch-even ch-ints)
;(a/filter<)
;(is (= (<!! ch-even) 2))
;(is (= (<!! ch-even) 4))
;(is (= (<!! ch-even) 6))

