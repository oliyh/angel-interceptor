(ns angel.interceptor
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires [interceptor & requirements]
  (assoc (pedestal-interceptor/interceptor interceptor) ::requires (set requirements)))

(defn provides [interceptor & provisions]
  (assoc (pedestal-interceptor/interceptor interceptor) ::provides (set provisions)))


(defn- reorder-interceptors [interceptors]
  (let [providers (reduce (fn [m i] (if-let [p (::provides i)]
                                     (into m (zipmap p (repeat i)))
                                     m)) {} interceptors)]

    (distinct (reduce (fn [c i]
                        (if-let [requirements (::requires i)]
                          (into c
                                (conj (mapv (fn [r] (if-let [p (get providers r)]
                                                      p
                                                      (throw (Exception.
                                                              (format
                                                               "No interceptor provides %s to satisfy %s"
                                                               r
                                                               (:name i))))))
                                            requirements)
                                      i))
                          (conj c i)))
                      [] interceptors))))

(defn- reorder-route-interceptors [routes]
  (map #(update % :interceptors reorder-interceptors) routes))

(defn satisfy [service-map]
  (cond-> service-map

    (:io.pedestal.http/interceptors service-map)
    (update :io.pedestal.http/interceptors reorder-interceptors)

    (:io.pedestal.http/routes service-map)
    (update :io.pedestal.http/routes reorder-route-interceptors)))
