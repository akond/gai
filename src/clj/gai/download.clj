(ns gai.download
	(:require
		[gai.structure :refer [questions-per-test]]
		[gai.logic :refer :all]
		[clojure.java.io :as io]
		[clj-http.client :as client]
		[clojure.zip :as z]
		[cheshire.core :refer [parse-string]]
		[hickory.core :refer [parse as-hiccup]])
	(:import
		(gai.structure Question Book)))


(defn throw-if [pred fmt & args]
	(when pred
		(throw (Exception. fmt))))


(defn pause [ms]
	(println "Waiting... for " ms)
	(Thread/sleep ms))


(defn download-test-json [^Book book n]
	(let [url (str (.json-url book) "/ru/questions/" n ".json")
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


(defn download-answers [question book]
	(doall (for [image (.images question)]
			   (let [url (str (.image-url book) "/" image)
					 file-name (str (.local-storage book) "/answers/" image)]
				   (save-url url file-name)))))


(defn download-title [question ^Book book]
	(let [title-file-name (.image question)
		  file-name (str (.local-storage book) "/title/" title-file-name)
		  url (str (.image-url book) "/" title-file-name)]
		(when-not (file-exists? file-name)
			(save-url url file-name))))


(defn fetch-test-json [^Book book id]
	(let [file (.json-file book id)]
		(if (file-exists? file)
			(-> file
				(slurp))
			(-> (download-test-json book id)
				(print-return "Fetching...")
				(save-json id file)))))


(defn json->Question [json id]
	(let [{:strs [que_title que_title_img que_answers que_answers_check que_answers_img que_help]} json]
		(Question. id
				   que_title
				   (if (not-empty que_title_img) (str (padded (id->test id)) "/" (padded (id->q id)) "." que_title_img))
				   (rest que_answers)
				   (map #(str (padded (id->test id)) "/" (padded (id->q id)) "_" (padded %2) "." %1) (filter (every-pred string? (complement empty?)) que_answers_img) (range 1 1000))
				   que_help)))


(defn- html->tags [s]
	(filter vector?
			(loop [t (z/next (z/vector-zip (first (as-hiccup (parse s)))))
				   r []]
				(if (z/end? t)
					r
					(recur (z/next t) (conj r (z/node t)))))))


(defn download-assets [question book]
	(let [image-tag? (fn [t] (#{:img} (first t)))
		  tags (html->tags (.-hint question))
		  images (map (comp :src second) (filter image-tag? tags))]
		(doseq [i images]
			(save-url (str (.url book) i) (str "./data" i))
			)))

(defn download-book [^Book book]
	(for [question-number (range (.number-of-questions book))]
		(let [id (inc question-number)]
			(try
				(println "[" id "]")

				(let [json (parse-string (fetch-test-json book id))
					  question (json->Question json id)]
					(doto question
						(download-title book)
						(download-answers book)
						(download-assets book)
						))

				(catch Exception e
					(prn "Ошибка: " (.getMessage e))
					(System/exit 0)))
			)))
