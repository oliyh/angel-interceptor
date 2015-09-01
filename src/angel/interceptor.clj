(ns angel.interceptor
  (:refer-clojure :exclude [prefers])
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires [interceptor & requirements]
  (assoc interceptor
         ::requires (set requirements)
         ::strict? true))

(defn prefers [interceptor & requirements]
  (assoc interceptor
         ::requires (set requirements)
         ::strict? false))

(defn provides [interceptor & provisions]
  (assoc interceptor ::provides (set provisions)))


(defn- reorder-interceptors [interceptors]
  (let [providers (reduce (fn [m i] (if-let [p (::provides i)]
                                      (into m (zipmap p (repeat i)))
                                      m)) {} interceptors)]

    (->> interceptors
         (reduce
          (fn [c i]
            (if-let [requirements (::requires i)]
              (into c
                    (conj
                     (->> requirements
                          (mapv (fn [r] (if-let [p (get providers r)]
                                          p
                                          (when (::strict? i)
                                            (throw (Exception.
                                                    (format
                                                     "No interceptor provides %s to satisfy %s"
                                                     r
                                                     (:name i))))))))
                          (remove nil?)
                          (into []))
                     i))
              (conj c i)))
          [])
         distinct
         (into []))))

(defn- reorder-route-interceptors [routes]
  (mapv #(try (update % :interceptors reorder-interceptors)
              (catch Exception e
                (throw (Exception. (str "Route" (pr-str %)) e))))
        (cond
          (seq? routes) routes
          (fn? routes) (routes))))

(defn satisfy [service-map]
  (cond-> service-map

    (:io.pedestal.http/interceptors service-map)
    (update :io.pedestal.http/interceptors reorder-interceptors)

    (:io.pedestal.http/routes service-map)
    (update :io.pedestal.http/routes reorder-route-interceptors)))
