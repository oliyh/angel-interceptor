(ns angel.interceptor
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route])
  (:refer-clojure :exclude [prefers]))

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

(defn conditional
  "Specifies that the interceptor is conditional on a predicate"
  [interceptor predicate]
  (vary-meta interceptor
             assoc
             ::conditional predicate))

(defn- strict-assertion [requirement interceptor]
  (when (-> interceptor meta ::strict?)
    (throw (Exception. (format "No interceptor provides %s to satisfy %s"
                               requirement
                               (:name interceptor))))))

(defn- remove-route-interceptors [interceptors]
  (filter (fn [interceptor]
            (let [pred (::conditional (meta interceptor))]
              (cond
                (nil? pred) true
                (fn? pred) (pred)
                :else pred)))
          interceptors))

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

(def apply-rules (comp reorder-interceptors remove-route-interceptors))

(defn- apply-route-rules [routes]
  (map #(try (update % :interceptors apply-rules)
             (catch Exception e
               (throw (Exception. (str "Route" (pr-str %)) e))))
       (if (satisfies? route/ExpandableRoutes routes)
         (route/expand-routes routes)
         routes)))

(defn satisfy
  "Given a pedestal service-map returns an updated map where dependencies between
   interceptors have been resolved by reordering"
  [service-map]
  (cond-> service-map

    (::bootstrap/interceptors service-map)
    (update ::bootstrap/interceptors apply-rules)

    (::bootstrap/routes service-map)
    (update ::bootstrap/routes
            (fn [routes]
              (cond
                (fn? routes) (comp apply-route-rules routes)
                (seq routes) (apply-route-rules routes))))))
