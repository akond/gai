(ns app.core
	(:require [clj-http.client :as client]
			  [cheshire.core :refer [parse-string]]
			  [clojure.java.io :as io]
			  [clojure.pprint :refer [pprint]]
			  [clojure.tools.reader.edn :as edn]))

(def number-of-tests 80)

(def questions-per-test 20)

;(defn fetch-test [n]
;	(let [response (client/get (str "http://gai.eu.com/assets/resource/exam/cache/2013_AB/ru/questions/" n ".json"))
;		  status (:status response)
;		  body (parse-string (:body response))]
;		(when (= 200 status)
;			body)))
;
;
;

(defn pause [ms]
	(println "Waiting... for " ms)
	(Thread/sleep ms))
;
;(defn save-test [n]
;	(let [file-name (str "data/" n ".json")
;		  java-file (io/as-file file-name)
;		  file-exists? (.exists java-file)
;		  file-empty? (= 0 (.length java-file))
;		  is-missing? (or (not file-exists?) file-empty?)]
;
;		(if is-missing?
;			(do
;				(println (str "Saving test #" n))
;				(let [body (fetch-test n)]
;					(if (= 0 (count body))
;						(println "Couldnot fetch the test " n)
;						(spit file-name body)))
;
;				(pause 300))
;			;(println "File" n " already exists")
;			)))
;
;
;(defn fetch-tests []
;	(doall
;		(for [n (range 1 (inc (* number-of-tests questions-per-test)))]
;			(save-test n))))
;
;
;
;
;(defn get-test [n]
;	(let [d (edn/read-string (slurp (str "data/question/" n ".json")))]
;		(merge
;			{:id   n
;			 :test (first (no->question n))
;			 :no   (second (no->question n))}
;			(zipmap (map (comp keyword #(subs % 4)) (keys d)) (vals d)))))
;
;(defn file-exists? [f]
;	(.exists (io/as-file f)))
;
;
;(defn save-url [url f]
;	(when-not (file-exists? f)
;		(let [response (client/get url {:as :byte-array})]
;			(when (= 200 (:status response))
;				(io/make-parents f)
;				(with-open [w (io/output-stream f)]
;					(.write w (:body response)))
;				(println "Saving " url " -> " f)
;				(Thread/sleep 300)
;				))))
;
;(defn dl-title-image [n ext]
;	(let [title-file-name (str (no->question n "/") "." ext)
;		  file-name (str "data/title/" title-file-name)
;		  url (str "http://gai.eu.com/assets/images/cards/2013_AB/" title-file-name)]
;		;(prn n title-file-name)
;
;		(save-url url file-name)
;		#_(when-not (file-exists? file-name)
;			  (let [title-image-url url
;					img (client/get title-image-url {:as :byte-array})]
;				  (when (= 200 (:status img))
;					  (prn title-image-url)
;					  (io/make-parents file-name)
;					  (with-open [w (io/output-stream file-name)]
;						  (.write w (:body img)))
;					  (Thread/sleep 300)
;					  )))))
;
;
;(defn dl-all-title-images []
;	(doall (for [test (range 80)
;				 question (range 20)]
;			   (let [n (inc (+ question (* test 20)))
;					 t (get-test n)
;					 ext (:title_img t)]
;				   (when-not (empty? ext)
;					   (dl-title-image n ext))))))
;
;
;(defn dl-answer-images [m]
;	(let [answers (:answers_img m)
;		  answer-extensions (filter (every-pred string? (complement empty?)) answers)]
;
;		(when-not (empty? answer-extensions)
;			(let [prefix (str (padded (:test m)) "/" (padded (:no m)) "_")
;				  image-urls (map-indexed (fn [i s]
;											  (str "http://gai.eu.com/assets/images/cards/2013_AB/" prefix (padded (inc i)) "." s))
;										  answer-extensions)
;				  files (map-indexed (fn [i s]
;										 (str "data/answers/" prefix (padded (inc i)) "." s))
;									 answer-extensions)]
;				(doall
;					(map (partial apply save-url) (partition 2 (interleave image-urls files))))))))
;
;(defn dl-all-answer-images []
;	(doall (for [test (range number-of-tests)
;				 no (range questions-per-test)]
;			   (let [id (+ 1 no (* 20 test))]
;				   (println "[" id "]")
;				   (dl-answer-images (get-test id))))))
;
;(defn all-tests [form]
;	(doall (for [test (range number-of-tests)
;				 no (range questions-per-test)]
;			   (let [id (+ 1 no (* 20 test))]
;				   (println "[" id "]")
;				   (form id)))))

(defrecord Exam [url num])

(def Kiev (Exam. "2013_AB_KIEV" 110))


(defn download-questions [exam]
	(doall (for [test (range (.num exam))
				 no (range questions-per-test)]
			   (let [id (+ 1 no (* 20 test))]
				   (println "[" id "]")
				   (form id))))
	)

(defn -main []
	(let []
		(println "Starting...")
		(prn (.url Kiev ))

		;(download-questions Kiev)
		;(dl-answer-images (get-test 324))
		;(println (no->question 324))
		;(println title-image-url)
		;
		;(def EXAM "2013_AB")

		#_
		(all-tests (fn [i]
					   (save-url
						   (str "http://gai.eu.com/assets/resource/exam/cache/2013_AB/ru/questions/" i ".json")
						   (str "data/q/" i ".json"))))



		;(println "http://gai.eu.com/assets/images/cards/2013_AB/18/05.jpg" (no->question 1002 "/"))
		(println "Finished")
		))
