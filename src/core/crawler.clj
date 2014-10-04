(ns core.crawler
  (:require [clojure.core.async :refer [chan >! <! >!! <!! alts!! close! go onto-chan] :as a])
  (:require [clojure.test :refer :all]))

;Main Exercise:
;Using Go blocks and Channels create a crawler that will asynchronously crawl an api
;For simplicity our Api is just an interface and a dummy remote-api implementation is provided.
;I recommend solving this exercise in a couple of steps.
;
;Step I:
; Integrate this callback based api so that you can test it using channels.
; Fix tests.
;
;Step II:
;You should be able to send one request to the requestsCh (e.g. 1).
;As a result a single body from Api should be registered in the answersCh receive port.
;Make sure that closing requestsCh causes the answersCh to receive the answer and closes answersCh, too!
;Write tests!
;
;Step III:
;Each body from Api might carry zero or more links. Combine the channels in such a way that
;the Crawler crawls the whole Api.
;Write tests!
;
;Step IV:
;The whole crawling thing might take looong. Add a timeout capability to the Crawler.
;After a specified amount of time no more requests to Api shall be sent; all channels get closed; the answer so far is
;available in answersCh
;
;Step V:
;Some of the links are of more interest to us then others. Since crawling can time out, we would like to get them first.
;Use java.util.concurrent.PriorityQueue or clojure.data.priority-map
;Make sure you do not block a go-block.
;Write tests with an example comparator!
;
;Step VI:
;Since the FiberApi suspends the Fiber, only one Api request is being processed concurrently.
;Change your Crawler in such a way that up to N requests can be processed concurrently.



(defn remote-api [size]
  (fn [link callback]
    (a/thread
      (if (> link (/ size 2))
        (do
          (Thread/sleep 50)
          (callback {:content (str "I am " link) :links []}))
        (do
          (Thread/sleep 50)
          (callback {:content (str "I am " link)
                     :links   [(* 2 link) (+ 1 (* 2 link))]}))))))

(defn <<< [f & args]
  (let [c (chan)]
    (apply f (concat args [(fn [x]
                             (if (nil? x)
                               (close! c)
                               (a/put! c x)))]))
    c))

(is (= (<!! (<<< (remote-api 5) 1))
       {:content "I am 1" :links [2 3]}))
(is (= (<!! (<<< (remote-api 5) 2))
       {:content "I am 2" :links [4 5]}))
(is (= (<!! (<<< (remote-api 5) 3))
       {:content "I am 3" :links []}))


(defn crawler

  ([start async-graph]
   (crawler start async-graph nil))

  ([start async-graph timeout]
   (let [requests-ch (chan 5)
         req-resp-ch (chan 10)
         answer-ch (a/reduce merge {} req-resp-ch)
         n-links-ch (chan 1)]
     (>!! requests-ch start)
     (>!! n-links-ch 1)

     (a/go-loop
       []
       (if-let [req (<! requests-ch)]
         (do
           (let [resp (<! (<<< async-graph req))
                 n-new-links (count (:links resp))]
             (>! req-resp-ch {req resp})
             (>! n-links-ch (dec n-new-links))
             (onto-chan requests-ch (:links resp) false)
             (recur)))
         (close! req-resp-ch)))

     (when-not (nil? timeout)
       (a/go
         []
         (<! (a/timeout timeout))
         (close! req-resp-ch)
         (close! requests-ch)
         (close! n-links-ch)))

     (a/go-loop
       [sum 0]
       (if-let [n (<! n-links-ch)]
         (let [new-sum (+ sum n)]
           (if (= 0 new-sum)
             (do
               (close! requests-ch)
               (close! n-links-ch))
             (recur new-sum)))))

     answer-ch)))

(def async-graph (remote-api 10))
;(<!! (crawler 1 async-graph))