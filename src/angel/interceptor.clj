(ns angel.interceptor
  (:require ;;[io.pedestal.http :as bootstrap]
   [io.pedestal.interceptor :as pedestal-interceptor]))

(defn requires [interceptor & requirements]
  (assoc (pedestal-interceptor/interceptor interceptor) ::requires (set requirements)))

(defn provides [interceptor & provisions]
  (assoc (pedestal-interceptor/interceptor interceptor) ::provides (set provisions)))

(defn satisfy [service-map])
