(ns gai.structure)


(def questions-per-test 20)


(defprotocol ExamResource
	(json-url [this])
	(image-url [this])
	(json-file [this id])
	(local-storage [this]))


(defrecord Book [title name url number-of-questions]
	ExamResource
	(json-url [this] (str (.url this) "/assets/resource/exam/cache/" (.name this)))
	(image-url [this] (str (.url this) "/assets/images/cards/" (.name this)))
	(local-storage [this] (str "data/" (.name this)))
	(json-file [this id] (str "data/" (.name this) "/q/" id ".json")))


(defrecord Question [id title image answers images hint])


(def Kiev (Book. "Киев 2013" "2013_AB_KIEV" "http://gai.eu.com" (* questions-per-test 110)))

(def Books [Kiev])
