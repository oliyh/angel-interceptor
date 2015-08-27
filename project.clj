(defproject angel-interceptor "0.1.0-SNAPSHOT"
  :description "Express dependencies for Pedestal interceptors"
  :url "https://github.com/oliyh/angel-interceptor"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot angel-interceptor.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
