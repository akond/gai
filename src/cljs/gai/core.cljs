(ns gai.core
	(:require-macros [cljs.core.async.macros :refer [go go-loop]])
	(:require [reagent.core :as r :refer [atom]]
			  [cljs-http.client :as http]
			  [cljs.core.async :refer [<! >! chan]]
			  [gai.logic :as logic :refer [tq->id id->test id->q padded no->question prefix]]
			  [gai.structure :refer [Books]]
			  [goog.string.newlines :as newlines]
			  [goog.array :as garray]
			  [gai.view :as view]
			  [alandipert.storage-atom :refer [local-storage]]))

(enable-console-print!)

(defonce active-project (atom ""))
(defonce problem-questions (atom []))
(defonce question-status (atom []))
(defonce active-question-id (atom nil))
(defonce active-question-data (atom nil))
(defonce exam-error-mode? (atom false))

(def prefs (local-storage (atom nil) :prefs))
(def last-viewed-test (local-storage (atom {}) :last-viewed-test))




(defn- last-viewed-id [error-mode?]
	(str (.-name @active-project) "/" (if error-mode? "error" "casual")))

(defn adjust-test [id json]
	(let [data json
		  [t q] (no->question id)
		  title-prefix (prefix (str "data/" (.-name @active-project) "/title"))
		  answer-prefix (prefix (str "data/" (.-name @active-project) "/answers"))
		  title-image (:que_title_img json)]

		{:id      id
		 :test    t
		 :no      q
		 :title   (:que_title json)
		 :answers (rest (:que_answers json))
		 :correct (inc (.indexOf (to-array (:que_answers_check json)) 1))
		 :image   (when (not-empty title-image) (title-prefix t q title-image))
		 :images  (map
					  (partial answer-prefix t q)
					  (filter (every-pred string? not-empty) (:que_answers_img json))
					  (range 1 1000))
		 :hint    (:que_help json)
		 }))


(defn adjusted-test-id [id]
	(if @exam-error-mode? (nth @prefs (dec id)) id))


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
					 (let [max-id (if @exam-error-mode? (count @prefs) (.-num @active-project))]
						 (min max-id (max 1 (+ n step)))))
			  id (adjusted-test-id (swap! active-question-id op))]
			(reset! active-question-data (adjust-test id (<! (load-question @active-project id))))
			(swap! last-viewed-test assoc (last-viewed-id @exam-error-mode?) @active-question-id)
			)))



(defn show-error [id ans]
	(reset! view/invalid-anser-was-given true))


(defn question->error [id]
	(swap! prefs (comp vec distinct (fnil conj [])) id))


(defn check-answer [question ans]
	{:pre [(number? ans)]}
	(let [correct? (= (str (:correct question)) (str ans))]
		(if correct?
			(move->next-question)
			(do
				(question->error (:id question))
				(show-error (:id question) ans))
			)))


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
		(reset! exam-error-mode? error-mode?)

		(go
			(let [id (adjusted-test-id @active-question-id)]
				(reset! active-question-data (adjust-test id (<! (load-question project id))))))
		))


(defn app []
	[:div
	 (case @view/mode
		 "initial" (list [view/file-loader-component on-file-load]
						 [view/navigation problem-questions]
						 [view/project-selector Books select-project])
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
