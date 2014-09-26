; If everything is OK, you should be able to copy-paste the following lines without error.

; Documentation: http://clojure.github.io/core.async/

; Official walkthrough: https://github.com/clojure/core.async/blob/master/examples/walkthrough.clj

; Rich Hickey: http://clojure.com/blog/2013/06/28/clojure-core-async-channels.html

(require '[clojure.core.async :refer [chan >! <! >!! <!! alts!! close! go onto-chan] :as a])
(require '[clojure.core.async.lab :as al])
(require '[clojure.test :refer :all])

; == Channels basics
; channel is like a fifo queue with a buffer
(def buffer-size 1)
(def ch1 (chan buffer-size))

; one can write to it
(>!! ch1 5)
; and read from it
(def read (<!! ch1))
(is (= read 5))

; ** Exercise 1a:
;   try in REPL: (<!! ch) what happens?
; ** Exercise 1b:
;   you can create an unbuferred channel by invoking (chan)
;   what is going to happen if you send there a lot of values?



; == Transformations of channels
; channels are fun because you can use map etc.
(def ch-int (chan 10))
(>!! ch-int 40)
(def ch-str (a/map str [ch-int]))
(is (= (<!! ch-str) "40"))


; Sometimes a transducer better suits your needs:
(def ch-str (chan 1 (map str)))
(>!! ch-str 40)
(is (= (<!! ch-str) "40"))

; ** Exercise 2:
;   Use filter transducer to create channel ch-even into which you serve integers but read only even numbers

(def ch-even)

(>!! ch-even 1)
(>!! ch-even 2)
(close! ch-even)
(is (= (<!! ch-even) 2))
(is (= (<!! ch-even) nil))



; thanks to transducers you can even reduce the whole channel into a single value
; ** Exercise 2a:
;   use a/reduce function in place of ???

(def ch-int (chan 3))
(def ch-sum ???)
(>!! ch-int 1)
(>!! ch-int 1)
(>!! ch-int 1)
(close! ch-int)                                             ; Note that we close! the ch-inc before reading from ch-sum !
(is (= (<!! ch-sum) 3))


; == Channels as means of inter thread communication
; We can safely communicate two threads using channels:

(def c1 (chan))
(a/thread (while true
            (let [v (<!! c1)]
              (println "Read" v))))
(a/thread (do
            (>!! c1 "hi")))

; And we can use alts!! to choose from which channel to read
(let [c1 (chan)
      c2 (chan)]
  (a/thread (while true
              (let [[v ch] (alts!! [c1 c2])]
                (println "Read" v "from" ch))))
  (a/thread (do
              (>!! c1 "hi")
              (>!! c2 "there"))))

; ** Exercise 3:
; Using alts!! change the implementation below so that the thread
; stops if a something gets pushed into poison-pills channel
(let [c1 (chan)
      poison-pills (chan)]
  (a/thread (while true
              (let [v (>!! c1 1)]
                (<! (timeout 100))
                (println "Read" v))))
  (>!! c1 "hi"))



; == Go Blocks
; The real power comes with Go blocks

(def c1 (chan))
(go (while true
      (let [v (<! c1)]
        (println "Read" v))))
(go
  (>! c1 "hi"))

; Compare this to the version from
; "Channels as means of inter thread communication" section
; The only diff is we substituted "thread"s by "go"s!
;
; Yet now the <! and >! operations do not block the thread executing a go block.
; Instead the execution is suspended and the thread moved to other tasks.


; == Testing Go Blocks
; One of the excellent parts of go blocks is that testing it is very easy
; because all you need is to put things on the input wire and read result from the output wire:

(def c1 (chan))
(def c2 (chan))

(go
  (loop [a 0]
    (if-let [v (<! c1)]
      (let [sum (+ v a)]
        (>! c2 sum)
        (recur sum)))))

(al/spool [1 1 1 3] c1)
(is (= (<!! c2) 1))
(is (= (<!! c2) 2))
(is (= (<!! c2) 3))
(is (= (<!! c2) 6))


; == Integration with callbacks

(defn async-operation [x callback]
  (a/thread
    (Thread/sleep 100)
    (callback (* x x))))

(defn <async-operation [x]
  (let [c (chan 1)]
    (async-operation x
                     (fn [result]
                       (do
                         (a/put! c result)
                         (close! c))))
    c))

(def chOut (chan))
(go
  (let [async-op-result (<! (<async-operation 5))]
    (>! chOut async-op-result)))

(is (= (<!! chOut) 25))


; one can also write a general version transforming functions with callbacks into channel-functions:
(defn <<< [f & args]
  (let [c (chan)]
    (apply f (concat args [(fn [x]
                             (if (nil? x)
                               (close! c)
                               (a/put! c x)))]))
    c))