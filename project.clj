(defproject angel-interceptor "0.3.0"
  :description "Express dependencies for Pedestal interceptors"
  :url "https://github.com/oliyh/angel-interceptor"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :target-path "target/%s"
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.8.0"]
                                       [io.pedestal/pedestal.service "0.5.0"]
                                       [javax.servlet/javax.servlet-api "3.1.0"]]}
             :dev {:dependencies []}})
