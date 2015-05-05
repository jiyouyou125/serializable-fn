(ns serializable.fn-test
  (:refer-clojure :exclude [fn])
  (:use [serializable.fn]
        [clojure.test]))

(def dinc-list '(serializable.fn/fn [x] (inc (inc x))))

(def dinc (eval dinc-list))

(deftest fns-created-with-metadata
  (is (ifn? dinc))
  (is (map? (meta dinc))))

(deftest metadata-fns-work-as-original
  (is (= 2 (dinc 0))))

(deftest metadata-fns-return-source
  (is (.contains (:serializable.fn/source (meta dinc))(str (first (drop 2 dinc-list))) )))

(deftest printing-fns-show-source
    (is (.contains (pr-str dinc) (str (first (drop 2 dinc-list))))))

(deftest preserve-reader-metadata
  (is (number? (:serializable.fn/line (meta dinc)))))

(def write+read (comp deserialize serialize))

(defn round-trip [f & args]
  (apply (write+read f) args))

(deftest serializable-fn-roundtrip!!!111eleven
  (is (= 2 (round-trip dinc 0))))

(deftest stacktraces-contain-line-col
  (try ((fn []
        (throw (RuntimeException.))))
       (catch RuntimeException e
         (is (re-matches #".*_test_sfn_\d{1,3}_\d{1,3}_.*" (str (first (.getStackTrace e))) )))))

(deftest roundtrip-with-lexical-nonconst-context
  (let [x 10, y (inc x)]
    (is (= 11
           (round-trip (fn [] y))))))

(deftest roundtrip-with-fnarg-context
  (is (= 11
         (round-trip ((fn [x]
                        (let [y (inc x)]
                          (fn [] y)))
                      10)))))

(deftest roundtrip-twice!
  (is (= 5
         ((write+read (write+read (let [x 5]
                                    (fn [] x))))))))
