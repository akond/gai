(ns common.structure)


(def questions-per-test 20)


(defprotocol ExamResource
	(json-url [this])
	(image-url [this])
	(json-file [this id])
	(local-storage [this]))


(defrecord Exam [title name num]
	ExamResource
	(json-url [this] (str "http://gai.eu.com/assets/resource/exam/cache/" (.name this)))
	(image-url [this] (str "http://gai.eu.com/assets/images/cards/" (.name this)))
	(local-storage [this] (str "data/" (.name this))))


(defrecord Question [id title image answers images])


(def Kiev (Exam. "Киев 2013" "2013_AB_KIEV" (* questions-per-test 110)))
(def Projects [Kiev])
