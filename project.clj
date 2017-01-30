(defproject app "0.1.0-SNAPSHOT"
	:description "FIXME: write description"
	:url "http://example.com/FIXME"
	:license {:name "Eclipse Public License"
			  :url  "http://www.eclipse.org/legal/epl-v10.html"}
	:dependencies [[org.clojure/clojure "1.8.0"]
				   [org.clojure/clojurescript "1.9.293"]
				   [clj-http "2.3.0"]
				   [cheshire "5.7.0"]
				   [org.clojure/tools.reader "1.0.0-beta4"]
				   [org.clojure/core.async "0.2.395"]
				   [reagent "0.6.0"]
				   [org.clojure/core.match "0.3.0-alpha4"]
				   [cljs-http "0.1.42"]]

	:plugins [[lein-figwheel "0.5.8"]
			  [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

	:cljsbuild {:builds
				[{:id           "dev"
				  :figwheel     true
				  :source-paths ["src"]
				  :compiler     {:main web.core
								 :source-map-timestamp true
								 :asset-path "js/compiled/out"
								 :output-to "resources/public/js/compiled/web.js"
								 :output-dir "resources/public/js/compiled/out"
								 }}]}


	:main app.core)
