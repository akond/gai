(ns app.core
	(:require [clj-http.client :as client]
			  [app.download :refer :all]
			  [common.structure :refer :all]
			  [clojure.java.io :as io]
			  [clojure.pprint :refer [pprint]]
			  [clojure.tools.reader.edn :as edn]))


(defn -main []
	(println "Starting...")
	(download-questions Kiev)
	(println "Finished"))
