(ns clj.app.core
	(:require [clj-http.client :as client]
			  [app.logic :refer :all]
			  [clojure.java.io :as io]
			  [clojure.pprint :refer [pprint]]
			  [clojure.tools.reader.edn :as edn]))



(defn -main []
	(println "Starting...")
	(download-questions Kiev)
	(println "Finished"))
