(ns gai.structure)


(def questions-per-test 20)


(defprotocol ExamResource
	(json-url [this])
	(image-url [this])
	(json-file [this id])
	(local-storage [this]))


(defrecord Book [title name number-of-questions]
	ExamResource
	(json-url [this] (str "http://gai.eu.com/assets/resource/exam/cache/" (.name this)))
	(image-url [this] (str "http://gai.eu.com/assets/images/cards/" (.name this)))
	(local-storage [this] (str "data/" (.name this)))
	(json-file [this id] (str "data/" (.name this) "/q/" id ".json")))


(defrecord Question [id title image answers images hint])


(def Kiev (Book. "Киев 2013" "2013_AB_KIEV" (* questions-per-test 110)))

(def Books [Kiev])
