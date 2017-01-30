(ns web.core
	(:require-macros [cljs.core.async.macros :refer [go go-loop]])
	(:require [reagent.core :as r :refer [atom]]
			  [cljs-http.client :as http]
			  [cljs.core.async :refer [<! >! chan]]
			  [app.logic :as logic :refer [tq->id id->test id->q padded no->question]]
			  [goog.string.newlines :as newlines]
			  [goog.array :as garray]
			  [cljs.core.match :refer-macros [match]]))

(enable-console-print!)

(defonce problem-questions (atom []))
(defonce question-status (atom []))
(defonce active-question-id (atom nil))
(defonce active-question-data (atom nil))
(defonce invalid-anser-was-given (atom false))

(defn load-question [id]
	(go
		(let [response (<! (http/get (str "data/q/" id ".json")))]
			(:body response))))

(defn move->next-question []
	(let [index (swap! active-question-id (fnil inc -1))
		  id (get @problem-questions index)]

		(go
			(reset! invalid-anser-was-given false)
			(reset! active-question-data (<! (load-question id))))))


(defn prefix [s]
	(fn [t q & [ext answer]]
		(match [(nil? answer)
				(nil? ext)]
			   [true true] (str s "/" (padded t) "/" (padded q))
			   [true false] (str s "/" (padded t) "/" (padded q) "." ext)
			   [false false] (str s "/" (padded t) "/" (padded q) "_" (padded answer) "." ext)
			   )))


(defn adjust-test [id json]
	(let [data (js->clj json)
		  [t q] (no->question id)
		  title-prefix (prefix "data/title")
		  answer-prefix (prefix "data/answers")
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
		 })
	)


(defn show-error [id ans]
	(swap! question-status assoc-in id 1)
	(reset! invalid-anser-was-given ans))

(defn check-answer [question ans]
	{:pre [(number? ans)]}
	(let [correct? (= (str (:correct question)) (str ans))]
		(if correct?
			(move->next-question)
			(show-error (:id question) ans))))

(defn question-component []
	(when-not (nil? @active-question-data)
		(let [x (adjust-test (get @problem-questions @active-question-id) @active-question-data)
			  title (:title x)
			  test-no (:test x)
			  q-no (:no x)
			  image (:image x)
			  images (:images x)
			  answers (:answers x)

			  id (:id x)]

			^{:key (str "t" id)}
			[:div
			 [:h2 {:style {:clear "both"}}
			  "Билет №" test-no
			  ", вопрос №" q-no]
			 [:h3 title]

			 (when image
				 [:div
				  [:img {:src image}]])

			 [:hr]

			 [:div
			  [:h4 "Возможные ответы:"]
			  [:ol
			   (doall (for [[idx answer] (map-indexed vector answers)]
						  (let [image (nth images idx nil)]
							  ^{:key (str "a" id "-" idx)}
							  [:li {:style {:padding "20px"}}
							   [:input {:type      "radio"
										:name      "answer"
										:value     (inc idx)
										:on-change #(check-answer x (js/parseInt (.-value (.-target %))))}]
							   answer
							   (when image
								   [:img {:src image}])
							   ])
						  ))]
			  ]

			 (when (number? @invalid-anser-was-given)
				 "invalid")
			 ])))


(defn load-test [t]
	(def c (chan 20))

	(doall (for [i (range 20)]
			   (let [q (inc i)
					 id (tq->id t q)]
				   (go (>! c (<! (load-question id))))
				   )))
	(go-loop [c c
			  res []]
			 (if (= 20 (count res))
				 res
				 (recur c (conj res (<! c)))))
	)


(defn filter-problem-questions [csv]
	(let [questions (flatten (for [line (newlines/splitLines csv)]
								 (clojure.string/split line #"," -1)))]
		(vec (map inc (filter number? (map-indexed #(when (pos? %2) %1) questions))))))




(defn load-new-problem-questions [csv]
	(reset! problem-questions (filter-problem-questions csv))
	(reset! question-status (repeat (count @problem-questions) :none))
	(reset! active-question-id nil)
	(move->next-question))


(defn skip-question []
	(if (< @active-question-id (count @problem-questions))
		[:div

		 [:button {:on-click move->next-question} "Пропустить"]]
		[:div "THE END"]))


(defn navigation []
	[:div {:style {:position "relative"}}
	 (doall (for [id @problem-questions]
				(let [q (id->q id)
					  t (id->test id)]
					[:span
					 {:style {
							  :padding "5px"
							  :border  "1px solid"
							  :float   "left"}}
					 t "." q])))])

(defn file-loader-component []
	[:div [:input {:type      "file"
				   :on-change (fn [e] (let [files (.-files (.-target e))
											quantity (.-length files)]
										  (doall (for [i (range quantity)]
													 (let [file (aget files i)
														   reader (js/FileReader.)]
														 (set! (.-onload reader) #(load-new-problem-questions (.-result reader)))
														 (.readAsText reader file)
														 )))
										  ))}]])

(defn app []
	[:div
	 [file-loader-component]
	 [navigation]
	 [question-component]
	 [skip-question]])

(r/render [app] (.getElementById js/document "app"))
