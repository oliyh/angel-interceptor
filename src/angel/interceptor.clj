(ns angel.interceptor
  (:refer-clojure :exclude [prefers])
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires [interceptor & requirements]
  (vary-meta interceptor
             assoc
             ::requires (set requirements)
             ::strict? true))

(defn prefers [interceptor & requirements]
  (vary-meta interceptor
             assoc
             ::requires (set requirements)
             ::strict? false))

(defn provides [interceptor & provisions]
  (vary-meta interceptor
             assoc
             ::provides (set provisions)))


(defn- reorder-interceptors [interceptors]
  (let [providers (reduce (fn [m i] (if-let [p (-> i meta ::provides)]
                                      (into m (zipmap p (repeat i)))
                                      m)) {} interceptors)]

    (->> interceptors
         (reduce
          (fn [c i]
            (if-let [requirements (-> i meta ::requires)]
              (into c
                    (conj
                     (->> requirements
                          (mapv (fn [r] (if-let [p (get providers r)]
                                          p
                                          (when (-> i meta ::strict?)
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
        routes))

(defn satisfy [service-map]
  (cond-> service-map

    (:io.pedestal.http/interceptors service-map)
    (update :io.pedestal.http/interceptors reorder-interceptors)

    (:io.pedestal.http/routes service-map)
    (update :io.pedestal.http/routes
            (fn [routes]
              (cond
                (fn? routes) #(reorder-route-interceptors (routes))
                (seq routes) (reorder-route-interceptors routes))))))
