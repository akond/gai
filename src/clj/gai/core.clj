(ns gai.core
	(:require [clj-http.client :as client]
			  [gai.structure :refer :all]
			  [gai.download :refer :all]
			  [clojure.java.io :as io]
			  [clojure.pprint :refer [pprint]]
			  [clojure.tools.reader.edn :as edn]))



(defn -main []
	(println "Starting...")
	(doall (download-book Kiev))
	(println "Finished"))
