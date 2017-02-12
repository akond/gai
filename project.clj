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
				   [alandipert/storage-atom "2.0.1"]
				   [org.clojure/core.match "0.3.0-alpha4"]
				   [cljs-http "0.1.42"]
				   [hickory "0.7.0"]
				   [thi.ng/color "1.2.0"]]

	:plugins [[lein-figwheel "0.5.8"]
			  [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

	:repl-options {:caught            clj-stacktrace.repl/pst+
				   :skip-default-init false
				   :host              "0.0.0.0"
				   :port              9000}

	:source-paths ["src/cljc" "src/clj" "src/cljs" "dev"]

	:cljsbuild {:builds
				[{:id           "dev"
				  :figwheel     true
				  :source-paths ["src/cljs" "src/cljc"]
				  :compiler     {:main                 gai.core
								 :source-map-timestamp true
								 :asset-path           "js/compiled/out"
								 :output-to            "resources/public/js/compiled/web.js"
								 :output-dir           "resources/public/js/compiled/out"
								 :preloads             [devtools.preload]
								 }}
				 {:id           "min"
				  :source-paths ["src/cljs" "src/cljc"]
				  :compiler     {:main          gai.core
								 :optimizations :advanced
								 :asset-path    "js/compiled/out"
								 :output-to     "resources/public/js/compiled/min.js"
								 :output-dir    "resources/public/js/compiled/min"
								 }}]}

	:profiles {:dev {:dependencies [[binaryage/devtools "0.8.2"]
									[figwheel-sidecar "0.5.8"]
									[com.cemerick/piggieback "0.2.1"]]
					 ;; need to add dev source path here to get user.clj loaded
					 :source-paths ["src" "dev"]
					 ;; for CIDER
					 ;;:plugins [[cider/cider-nrepl "0.12.0"]]
					 :repl-options {; for nREPL dev you really need to limit output
									:init             (set! *print-length* 50)
									:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}


	:main gai.core)
