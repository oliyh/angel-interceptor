(ns angel.interceptor
  (:refer-clojure :exclude [prefers])
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires
  "Specifies that the interceptor requires certain values to be provided
   and will fail if they are not met."
  [interceptor & requirements]
  (vary-meta interceptor
             assoc
             ::requires (set requirements)
             ::strict? true))

(defn prefers
  "Specifies that the interceptor prefers certain values to be provided
   but will continue if they are not met."
  [interceptor & requirements]
  (vary-meta interceptor
             assoc
             ::requires (set requirements)
             ::strict? false))

(defn provides
  "Specifies that the interceptor provides certain values"
  [interceptor & provisions]
  (vary-meta interceptor
             assoc
             ::provides (set provisions)))

(defn- strict-assertion [requirement interceptor]
  (when (-> interceptor meta ::strict?)
    (throw (Exception. (format "No interceptor provides %s to satisfy %s"
                               requirement
                               (:name interceptor))))))

(defn- reorder-interceptors
  "Produces an interceptor chain where the interceptors which provide values
   are placed before the interceptors which require them"
  [interceptors]
  (let [providers (reduce (fn [m i] (if-let [p (-> i meta ::provides)]
                                      (into m (zipmap p (repeat i)))
                                      m)) {} interceptors)]

    (->> interceptors
         (reduce
          (fn [chain interceptor]
            (if-let [requirements (-> interceptor meta ::requires)]
              (into chain
                    (conj
                     (->> requirements
                          (keep #(or (get providers %)
                                     (strict-assertion % interceptor)))
                          (into []))
                     interceptor))
              (conj chain interceptor)))
          [])
         distinct
         (into []))))

(defn- reorder-route-interceptors [routes]
  (mapv #(try (update % :interceptors reorder-interceptors)
              (catch Exception e
                (throw (Exception. (str "Route" (pr-str %)) e))))
        routes))

(defn satisfy
  "Given a pedestal service-map returns an updated map where dependencies between
   interceptors have been resolved by reordering"
  [service-map]
  (cond-> service-map

    (:io.pedestal.http/interceptors service-map)
    (update :io.pedestal.http/interceptors reorder-interceptors)

    (:io.pedestal.http/routes service-map)
    (update :io.pedestal.http/routes
            (fn [routes]
              (cond
                (fn? routes) #(reorder-route-interceptors (routes))
                (seq routes) (reorder-route-interceptors routes))))))
