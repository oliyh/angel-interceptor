(ns angel.interceptor
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires [interceptor & requirements]
  (assoc (pedestal-interceptor/interceptor interceptor) ::requires (set requirements)))

(defn provides [interceptor & provisions]
  (assoc (pedestal-interceptor/interceptor interceptor) ::provides (set provisions)))


(defn- reorder-interceptors [interceptors]
  (let [provides (reduce (fn [m i] (if-let [p (::provides i)]
                                     (into m (zipmap p (repeat i)))
                                     m)) {} interceptors)]

    (clojure.pprint/pprint provides)

    (distinct (reduce (fn [c i]
                        (if-let [r (::requires i)]
                          (do (println "requirement for" r)
                            (concat c (map provides r) [i]))
                          (conj c i)))
                      [] interceptors))))

(defn satisfy [service-map]
  (update service-map :io.pedestal.http/interceptors reorder-interceptors))
