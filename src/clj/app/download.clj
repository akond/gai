(ns clj.app.download
	(:require
		[app.structure :refer :all]
		[app.logic :refer :all]
		[cheshire.core :refer [parse-string]]))


(defn throw-if [pred fmt & args]
	(when pred
		(throw (Exception. fmt))))


(defn pause [ms]
	(println "Waiting... for " ms)
	(Thread/sleep ms))


(defn download-test-json [exam n]
	(let [url (str (.json-url exam) "/ru/questions/" n ".json")
		  response (client/get url)
		  status (:status response)
		  body (:body response)]
		(throw-if (not= 200 status) (str "Не удалось скачать файл " url))
		body))


(defn file-exists? [f]
	(.exists (io/as-file f)))


(defn save-json [json id file]
	(io/make-parents file)
	(spit file json)
	json)


(defn save-url [url f]
	(when-not (file-exists? f)
		(let [response (client/get url {:as :byte-array})]
			(when (= 200 (:status response))
				(io/make-parents f)
				(with-open [w (io/output-stream f)]
					(.write w (:body response)))
				(println "Saving " url " -> " f)
				(Thread/sleep 300)))))


(defn download-answers [question exam]
	(doall (for [image (.images question)]
			   (let [url (str (.image-url exam) "/" image)
					 file-name (str (.local-storage exam) "/answers/" image)]
				   (save-url url file-name))))

	question)


(defn download-title [question exam]
	(let [title-file-name (.image question)
		  file-name (str (.local-storage exam) "/title/" title-file-name)
		  url (str (.image-url exam) "/" title-file-name)]
		(when-not (file-exists? file-name)
			(save-url url file-name)))
	question)


(defn fetch-test-json [exam id]
	(let [file (json-file (.local-storage exam) id)]
		(if (file-exists? file)
			(-> file
				(slurp))
			(-> (download-test-json exam id)
				(print-return "Fetching...")
				(save-json id file)))))


(defn json->Question [json exam id]
	(let [fields ["que_title" "que_title_img" "que_answers" "que_answers_check" "que_answers_img"]
		  [title image answers correct images] (map json fields)]
		(Question. id
				   title
				   (if (not-empty image) (str (padded (id->test id)) "/" (padded (id->q id)) "." image))
				   (rest answers)
				   (map #(str (padded (id->test id)) "/" (padded (id->q id)) "_" (padded %2) "." %1) (filter (every-pred string? (complement empty?)) images) (range 1 1000)))))


(defn download-questions [exam]
	(doall (for [test (range (.num exam))
				 no (range questions-per-test)]
			   (let [id (+ 1 no (* 20 test))]
				   (try
					   (println "[" id "]")

					   (-> (fetch-test-json exam id)
						   (parse-string)
						   (json->Question exam id)
						   (download-title exam)
						   (download-answers exam))

					   (catch Exception e
						   (prn "Ошибка: " (.getMessage e))
						   (System/exit 0)))
				   ))))
