(ns gai.sample
    (:use [hickory.core]))

(defn -main []
(def a (as-hiccup (parse "<a href=\"foo\">foo</a>")))
(vector-zip (first a))
(prn a))

