(ns angel.interceptor-test
  (:require [clojure.test :refer :all]
            [angel.interceptor :as angel]
            [io.pedestal.interceptor :as pedestal-interceptor]
            [io.pedestal.interceptor.helpers :refer [before]]))

(defn- something [_] nil)
(def ^:private some-interceptor (before ::some-interceptor something))
(def ^:private another-interceptor (before ::another-interceptor something))
(def ^:private yet-another-interceptor (before ::yet-another-interceptor something))

(deftest requires-test
  (testing "works on plain map"
    (is (= (pedestal-interceptor/interceptor {:enter something
                                              ::angel/requires #{:authentication :authorisation}})
           (angel/requires {:enter something} :authentication :authorisation))))

  (testing "works on something that's already an interceptor"
    (is (= (pedestal-interceptor/interceptor {:enter something
                                              ::angel/requires #{:authentication :authorisation}})
           (angel/requires (before something) :authentication :authorisation)))))

(deftest provides-test
  (testing "works on plain map"
    (is (= (pedestal-interceptor/interceptor {:enter something
                                              ::angel/provides #{:authentication :authorisation}})
           (angel/provides {:enter something} :authentication :authorisation))))

  (testing "works on something that's already an interceptor"
    (is (= (pedestal-interceptor/interceptor {:enter something
                                              ::angel/provides #{:authentication :authorisation}})
           (angel/provides (before something) :authentication :authorisation)))))


(deftest satisfy-test

  (testing "leaves interceptors without requirements alone"
    (let [service-map {:io.pedestal.http/interceptors [some-interceptor another-interceptor]}]
      (is (= service-map
             (angel/satisfy service-map)))))

  (testing "reorders interceptors to put requires after the appropriate requires"
    (let [first-interceptor (angel/provides some-interceptor :something)
          second-interceptor (angel/requires another-interceptor :something)]

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor first-interceptor]})))))

  (testing "deals with multi-level dependencies"
    (let [first-interceptor (angel/provides some-interceptor :something)
          second-interceptor (angel/provides (angel/requires another-interceptor :something) :another-thing)
          third-interceptor (angel/requires yet-another-interceptor :another-thing)]

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor third-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor third-interceptor first-interceptor]})))))

  (testing "deals with multiple dependencies"
    (let [first-interceptor (angel/provides some-interceptor :something)
          second-interceptor (angel/provides another-interceptor :another-thing)
          third-interceptor (angel/requires yet-another-interceptor :something :another-thing)]

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor third-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor third-interceptor first-interceptor]})))))

  (testing "deals with repeated dependencies"
    (let [first-interceptor (angel/provides some-interceptor :something)
          second-interceptor (angel/requires another-interceptor :something)
          third-interceptor (angel/requires yet-another-interceptor :something)]

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor third-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor first-interceptor third-interceptor]})))

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor third-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor third-interceptor first-interceptor]})))

      (is (= {:io.pedestal.http/interceptors [first-interceptor third-interceptor second-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [third-interceptor second-interceptor first-interceptor]}))))))
