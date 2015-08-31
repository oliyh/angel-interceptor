(ns angel.interceptor-test
  (:require [clojure.test :refer :all]
            [angel.interceptor :as angel]
            [io.pedestal.interceptor :as pedestal-interceptor]
            [io.pedestal.interceptor.helpers :refer [before]]))

(defn- some-fn [_]
  nil)

(deftest requires-test
  (testing "works on plain map"
    (is (= (pedestal-interceptor/interceptor {:enter some-fn
                                              ::angel/requires #{:authentication :authorisation}})
           (angel/requires {:enter some-fn} :authentication :authorisation))))

  (testing "works on something that's already an interceptor"
    (is (= (pedestal-interceptor/interceptor {:enter some-fn
                                              ::angel/requires #{:authentication :authorisation}})
           (angel/requires (before some-fn) :authentication :authorisation)))))

(deftest provides-test
  (testing "works on plain map"
    (is (= (pedestal-interceptor/interceptor {:enter some-fn
                                              ::angel/provides #{:authentication :authorisation}})
           (angel/provides {:enter some-fn} :authentication :authorisation))))

  (testing "works on something that's already an interceptor"
    (is (= (pedestal-interceptor/interceptor {:enter some-fn
                                              ::angel/provides #{:authentication :authorisation}})
           (angel/provides (before some-fn) :authentication :authorisation)))))
