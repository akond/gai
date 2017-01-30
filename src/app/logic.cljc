(ns app.logic
	(:refer-clojure :exclude [format])
	#?(:cljs (:require [goog.string :as gstring :refer [subs]]
				 [goog.string.format])))

#?(:cljs (def format gstring/format)
   :clj  (def format clojure.core/format))


(defn id->test [n]
	(inc (quot (dec n) 20)))


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

;#?(:cljs
;   )
