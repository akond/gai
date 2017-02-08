(ns gai.logic
	(:refer-clojure :exclude [format])
	
	#?(:cljs
	   (:require [goog.string :as gstring]
		   [goog.string.format]
		   [clojure.zip :as z]
		   [hickory.core :refer [parse as-hiccup]]
		   [cljs.core.match :refer-macros [match]])
	   :clj
	   (:require
		   [clojure.zip :as z]
		   [hickory.core :refer [parse as-hiccup]]
		   [clojure.core.match :refer [match]])))

#?(:cljs (def ^{:private true} format gstring/format)
   :clj  (def ^{:private true} format clojure.core/format))



(defn id->test [n]
	(inc (quot (dec n) 20)))


(defn abs [n] (max n (- n)))


(defn id->q [n]
	(if (= 0 (mod n 20))
		20
		(mod n 20)))


(defn tq->id [t q]
	(+ q (* (dec t) 20)))


(defn padded [n]
	(format "%02d" n))


(defn no->question
	([n]
	 [(id->test n)
	  (id->q n)])
	([n glue]
	 (clojure.string/join glue [(padded (id->test n)) (padded (id->q n))])))


(defn prefix [s]
	(fn [t q & [ext answer]]
		(match [(nil? answer)
				(nil? ext)]
			   [true true] (str s "/" (padded t) "/" (padded q))
			   [true false] (str s "/" (padded t) "/" (padded q) "." ext)
			   [false false] (str s "/" (padded t) "/" (padded q) "_" (padded answer) "." ext)
			   )))

(defn print-return [x & rest]
	(when-not (empty? rest)
		(apply println rest))
	x)

(defn html->tags [s]
	(filter vector?
			(loop [t (z/next (z/vector-zip (first (as-hiccup (parse s)))))
				   r []]
				(if (z/end? t)
					r
					(recur (z/next t) (conj r (z/node t)))))))