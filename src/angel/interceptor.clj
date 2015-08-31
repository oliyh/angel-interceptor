(ns angel.interceptor
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires [interceptor & requirements]
  (assoc (pedestal-interceptor/interceptor interceptor) ::requires (set requirements)))

(defn provides [interceptor & provisions]
  (assoc (pedestal-interceptor/interceptor interceptor) ::provides (set provisions)))


(defn- reorder-interceptors [interceptors strict?]
  (let [providers (reduce (fn [m i] (if-let [p (::provides i)]
                                     (into m (zipmap p (repeat i)))
                                     m)) {} interceptors)]

    (into []
     (distinct (reduce (fn [c i]
                         (if-let [requirements (::requires i)]
                           (into c
                                 (conj (mapv (fn [r] (if-let [p (get providers r)]
                                                       p
                                                       (when strict?
                                                         (throw (Exception.
                                                                 (format
                                                                  "No interceptor provides %s to satisfy %s"
                                                                  r
                                                                  (:name i)))))))
                                             requirements)
                                       i))
                           (conj c i)))
                       [] interceptors)))))

(defn- reorder-route-interceptors [routes strict?]
  (mapv #(try (update % :interceptors reorder-interceptors strict?)
              (catch Exception e
                (throw (Exception. (str "Route" (pr-str %)) e))))
        (cond
          (seq? routes) routes
          (fn? routes) (routes))))

(defn satisfy [service-map & [strict?]]
  (cond-> service-map

    (:io.pedestal.http/interceptors service-map)
    (update :io.pedestal.http/interceptors reorder-interceptors strict?)

    (:io.pedestal.http/routes service-map)
    (update :io.pedestal.http/routes reorder-route-interceptors strict?)))
