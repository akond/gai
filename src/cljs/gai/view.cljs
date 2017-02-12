(ns gai.view
	(:require-macros
		[cljs.core.async.macros :refer [go go-loop]])
	(:require
		[reagent.core :as r :refer [atom]]
		[cljs.core.async :refer [put! chan <! >! timeout close!]]
		[gai.logic :refer [id->q id->test abs]]
		[goog.dom.ViewportSizeMonitor]
		[goog.math.Box]
		[alandipert.storage-atom :refer [local-storage]]
		[thi.ng.color.core :as col]))


(defonce mode (r/atom "initial"))
(defonce invalid-anser-was-given (atom false))
(defonce show-hint (atom false))
(defonce exam-error-mode? (atom false))
(def errors (local-storage (atom nil) :prefs))


(defn px [x]
	(str x "px"))

(def header-styles {:font-family "Open Sans"
					:font-size   (px 20)})

(defn file-loader-component [on-change]
	[:div [:input {:type      "file"
				   :on-change on-change}]])


(defn navigation [problem-questions]
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


(defn navigation-button [step on-click]
	(let [direction (if (neg? step) "-" "+")]
		^{:key (str "button" step)}
		[:button {:on-click (partial on-click step)} direction " " (abs step)]))


(defn navigation-buttons [to-the-beginning on-click]
	[:div {:style {:clear "both"}}
	 [:button {:on-click to-the-beginning} "В начало"]
	 [navigation-button -200 on-click]
	 [navigation-button -80 on-click]
	 [navigation-button -20 on-click]
	 [navigation-button -10 on-click]
	 [navigation-button -1 on-click]
	 [navigation-button 1 on-click]
	 [navigation-button 10 on-click]
	 [navigation-button 20 on-click]
	 [navigation-button 80 on-click]
	 [navigation-button 200 on-click]])


(defn project-selector [projects on-project-selected error-count]
	[:div {:style
		   {:border  "1px solid"
			:padding "20px"}}
	 (doall (for [project projects]
				^{:key (.-name project)}
				[:div
				 [:a {:href     "#"
					  :id       (.-name project)
					  :on-click (fn [e] (on-project-selected project false) false)}
				  (.-title project)]

				 (when (pos? error-count)
					 [:span
					  " -> "
					  [:a {:href     "#"
						   :id       (.-name project)
						   :on-click (fn [e] (on-project-selected project true) false)}
					   "работа над ошибками (" error-count ")"
					   ]]
					 )
				 ]
				))])



(def invalid-answer
	(r/create-class
		{:component-did-mount
		 (fn [e] (go (<! (timeout 1500))
					 (reset! invalid-anser-was-given false)
					 (doall (for [x (array-seq (.getElementsByTagName js/document "input"))]
								(aset x "checked" false)))))
		 :reagent-render
		 (fn []
			 (let [viewport-size (.getSize (new goog.dom.ViewportSizeMonitor))
				   viewport-width (.-width viewport-size)
				   viewport-height (min viewport-width (.-height viewport-size))
				   scaled (.scale viewport-size 0.35)
				   width (.-width scaled)
				   height (min (.-height scaled) 100 width)]

				 [:h1 {:style
					   {:color            "red"
						:position         "absolute"
						:border           "1px solid"
						:width            (px width)
						:height           (px height)
						:left             (px (- (/ viewport-width 2) (/ width 2)))
						:top              (px (- (/ viewport-height 2) (/ height 2)))
						:text-align       "center"
						:padding          "20px"
						:background-color "white"
						}}
				  [:span {:style {:line-height (px height)}}
				   [:span {:style {:animation "blinker 1s linear infinite"}} "Не правильно"]]]
				 ))}))


(defn hint-component [hint mark-error]
	(if @show-hint
		(do
			(mark-error)
			hint)
		[:button {:on-click #(reset! show-hint true)} "Подсказка"]))


(defn question-component [question check-answer mark-error]
	(let [{:keys [title test image images answers hint id]} @question]
		^{:key (str "t" id)}
		[:div
		 [:div {:style (into header-styles {:font-size (px 20)})} title]
		 (when image
			 [:div
			  [:img {:src image}]])

		 [:div {:style {:font-family "Roboto Condensed"}}
		  [:div {:style {:float "left" :width "70%"}}
		   [:ol
			(doall (for [[idx answer] (map-indexed vector answers)]
					   (let [image (nth images idx nil)]
						   ^{:key (str "a" id "-" idx)}
						   [:li {:style {:padding "20px"}}
							[:input {:type      "radio"
									 :name      "answer"
									 :value     (inc idx)
									 :on-change #(check-answer @question (js/parseInt (.-value (.-target %))))}]
							" " answer
							(when image
								[:img {:src image}])
							])))]]
		  [:div {:style {:width "30%" :float "left"}} [hint-component hint mark-error]]]

		 (when @invalid-anser-was-given
			 [invalid-answer])
		 ]))


(defn colorful-progress [no]
	{:pre [(number? no)]}
	(let [W (/ 100 20)
		  colors (cycle ["#FFE500"])
		  gray-colors (cycle ["#ADADAD" "#BDBDBD"])]
		[:svg {:height   "20px"
			   :width    "100%"
			   :view-box "0 0 100 2"}
		 (map #(let [left (* %2 W)
					 right (+ left W)
					 color (-> (col/css %1) col/as-hsva)
					 luminosity (.-v color)
					 tweaked-color (if (< 0.5 luminosity) (col/adjust-brightness color (- 0 luminosity -0.1))
														  (col/adjust-brightness color (- 0.9 luminosity)))]

				   ^{:key (str "cfp-" %2)}
				   [:g [:path {:d    (str "M " left ",0 L" right ",0 L" right ",2 L" left ",2 L" left ",0")
							   :fill %1}]
					[:text {:x           (+ 1.9 left) :y 1.7
							:fill        @(-> tweaked-color col/as-rgba col/as-css)
							:stroke      "none"
							:font-size   1.7
							:font-weight "bold"
							:font-family "Roboto Condensed"} (inc %2)]]) (concat (take no colors) (take (- 20 no) gray-colors)) (range 20))
		 ])
	)


(defn progress [project active-question-data]
	(let [{:keys [test no id]} @active-question-data
		  no-of-total (if @exam-error-mode? (str (inc (.indexOf @errors id)) "/" (count @errors)) (str test "/" no))]
		[:div
		 [:div {:style (into header-styles {:width "80%"
											:float "left"})}
		  (.-title @project) (if @exam-error-mode? " (ошибки)") " > " no-of-total]
		 [:div {:style (into header-styles {:text-align "right"})} id]

		 (when-not @exam-error-mode? [colorful-progress no])

		 [:hr {:style {:clear "both"}}]])
	)


(defn project [project active-question-data check-answer mark-error on-skip-question to-the-beginning]
	(let [{:keys [test no id]} @active-question-data]
		[:div
		 [progress project active-question-data]
		 [navigation-buttons to-the-beginning on-skip-question]
		 [:hr]
		 [question-component active-question-data check-answer mark-error]]
		))
