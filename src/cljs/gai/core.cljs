(ns gai.core
	(:require-macros [cljs.core.async.macros :refer [go go-loop]])
	(:require [reagent.core :as r :refer [atom]]
			  [clojure.zip :as zip :refer [vector-zip end? root edit rightmost down right node]]
			  [cljs-http.client :as http]
			  [cljs.core.async :refer [<! >! chan]]
			  [gai.logic :as logic :refer [tq->id id->test id->q padded no->question prefix html->tags]]
			  [gai.structure :refer [Books]]
			  [goog.string.newlines :as newlines]
			  [goog.array :as garray]
			  [hickory.core :refer [parse as-hiccup parse-fragment]]
			  [gai.view :as view]
			  [alandipert.storage-atom :refer [local-storage]])
	(:import [clojure.zip]))

(enable-console-print!)

(defonce active-project (atom ""))
(defonce problem-questions (atom []))
(defonce question-status (atom []))
(defonce active-question-id (atom nil))
(defonce active-question-data (atom nil))


(def last-viewed-test (local-storage (atom {}) :last-viewed-test))


(extend-type js/NodeList
	ISeqable
	(-seq [array] (array-seq array 0)))


(defn- last-viewed-id [error-mode?]
	(str (.-name @active-project) "/" (if error-mode? "error" "casual")))


(defn- +data-prefix [abc]
	(let [insert-tag (fn [e] (update e :src (fn [s] (str "/data" s))))]
		(loop [t (zip/next (vector-zip abc))]
			(if (end? t) (root t) (recur (zip/next (edit t #(if (and (map? %) (contains? % :src)) (insert-tag %) %))))))))


(defn adjust-test [id json]

	(let [[t q] (no->question id)
		  title-prefix (prefix (str "data/" (.-name @active-project) "/title"))
		  answer-prefix (prefix (str "data/" (.-name @active-project) "/answers"))
		  {:keys [que_title que_title_img que_help que_answers_img que_answers_check que_answers]} json
		  ]

		{:id      id
		 :test    t
		 :no      q
		 :title   que_title
		 :answers (rest que_answers)
		 :correct (inc (.indexOf (to-array que_answers_check) 1))
		 :image   (if-not (empty? que_title_img) (title-prefix t q que_title_img))
		 :images  (map
					  (partial answer-prefix t q)
					  (filter (every-pred string? not-empty) que_answers_img)
					  (range 1 1000))
		 :hint    (-> (vector-zip (first (as-hiccup (parse (str "<div>" que_help "</div>"))))) down rightmost down rightmost node +data-prefix)
		 }))




(defn adjusted-test-id [id]
	(if @view/exam-error-mode? (nth @view/errors (dec id)) id))


(defn load-question [project id]
	(go
		(let [response (<! (http/get (str "/data/" (.-name project) "/q/" id ".json")))]
			(if (string? (:body response))
				(let [raw-data (js->clj (.parse js/JSON (:body response)))]
					(zipmap (map keyword (keys raw-data)) (vals raw-data)))
				(js->clj (:body response))))))


(defn move->next-question [& [step]]
	(reset! view/invalid-anser-was-given false)
	(reset! view/show-hint false)
	(go
		(let [step (or step 1)
			  op (fn [n]
					 (let [max-id (if @view/exam-error-mode? (count @view/errors) (.-num @active-project))]
						 (min max-id (max 1 (+ n step)))))
			  id (adjusted-test-id (swap! active-question-id op))
			  adjusted-test (adjust-test id (<! (load-question @active-project id)))]
			;(prn adjusted-test)
			(reset! active-question-data adjusted-test)
			(swap! last-viewed-test assoc (last-viewed-id @view/exam-error-mode?) @active-question-id)
			)))



(defn show-error [id ans]
	(reset! view/invalid-anser-was-given true))


(defn question->error [id]
	(swap! view/errors (comp vec distinct (fnil conj [])) id))


(defn check-answer [question ans]
	{:pre [(number? ans)]}
	(let [correct? (= (str (:correct question)) (str ans))]
		(if correct?
			(move->next-question)
			(do
				(question->error (:id question))
				(show-error (:id question) ans)
				(reset! view/show-hint true)
				))))


(defn filter-problem-questions [csv]
	(let [questions (flatten (for [line (newlines/splitLines csv)]
								 (clojure.string/split line #"," -1)))]
		(vec (map inc (filter number? (map-indexed #(when (pos? %2) %1) questions))))))


(defn load-new-problem-questions [csv]
	(reset! problem-questions (filter-problem-questions csv))
	(reset! question-status (repeat (count @problem-questions) :none))
	(reset! active-question-id nil)
	(move->next-question))


(defn on-file-load [e]
	(let [files (.-files (.-target e))
		  quantity (.-length files)]
		(doall (for [i (range quantity)]
				   (let [file (aget files i)
						 reader (js/FileReader.)]
					   (set! (.-onload reader) #(load-new-problem-questions (.-result reader)))
					   (.readAsText reader file)
					   )))))


(defn on-skip-question [step]
	(move->next-question step))



(defn select-project [project error-mode?]
	(let [storage-id (last-viewed-id error-mode?)
		  first-id (or (get @last-viewed-test storage-id) 1)]

		(reset! view/mode "project")
		(reset! active-question-data nil)
		(reset! active-question-id first-id)
		(reset! active-project project)
		(reset! view/exam-error-mode? error-mode?)

		(go
			(let [id (adjusted-test-id @active-question-id)
				  adjusted-value (adjust-test id (<! (load-question project id)))]
				(reset! active-question-data adjusted-value)))
		))


(defn app []
	[:div
	 (case @view/mode
		 "initial" (list [view/file-loader-component on-file-load]
						 [view/navigation problem-questions]
						 [view/project-selector Books select-project (count @view/errors)])
		 "project" (view/project
					   active-project
					   active-question-data
					   check-answer
					   (partial question->error (:id @active-question-data))
					   on-skip-question
					   (fn [] (reset! view/mode "initial"))))

	 (when (< @active-question-id (count @problem-questions))
		 [view/navigation-buttons move->next-question])
	 ])

(r/render [app] (.getElementById js/document "app"))

