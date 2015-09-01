(ns angel.interceptor-test
  (:require [clojure.test :refer :all]
            [angel.interceptor :as angel]
            [io.pedestal.interceptor :as pedestal-interceptor]
            [io.pedestal.interceptor.helpers :refer [before]]
            [io.pedestal.http.route.definition :refer [defroutes]]))

(defn- something [_] nil)
(def ^:private some-interceptor (before ::some-interceptor something))
(def ^:private another-interceptor (before ::another-interceptor something))
(def ^:private yet-another-interceptor (before ::yet-another-interceptor something))

(deftest requires-test
  (testing "works on plain map"
    (is (= {::angel/requires #{:authentication :authorisation}
            ::angel/strict? true}
           (meta (angel/requires {:enter something} :authentication :authorisation)))))

  (testing "works on something that's already an interceptor"
    (is (= {::angel/requires #{:authentication :authorisation}
            ::angel/strict? true}
           (meta (angel/requires (before something) :authentication :authorisation))))))

(deftest prefers-test
  (testing "works on plain map"
    (is (= {::angel/requires #{:authentication :authorisation}
            ::angel/strict? false}
           (meta (angel/prefers {:enter something} :authentication :authorisation)))))

  (testing "works on something that's already an interceptor"
    (is (= {::angel/requires #{:authentication :authorisation}
            ::angel/strict? false}
           (meta (angel/prefers (before something) :authentication :authorisation))))))

(deftest provides-test
  (testing "works on plain map"
    (is (= {::angel/provides #{:authentication :authorisation}}
           (meta (angel/provides {:enter something} :authentication :authorisation)))))

  (testing "works on something that's already an interceptor"
    (is (= {::angel/provides #{:authentication :authorisation}}
           (meta (angel/provides (before something) :authentication :authorisation))))))


(deftest satisfy-test

  (testing "leaves interceptors without requirements alone"
    (let [service-map {:io.pedestal.http/interceptors [some-interceptor another-interceptor]}]
      (is (= service-map
             (angel/satisfy service-map)))))

  (testing "reorders interceptors to put requires after the appropriate provides"
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
             (angel/satisfy {:io.pedestal.http/interceptors [first-interceptor third-interceptor second-interceptor]})))))

  (testing "deals with repeated dependencies"
    (let [first-interceptor (angel/provides some-interceptor :something)
          second-interceptor (angel/requires another-interceptor :something)
          third-interceptor (angel/requires yet-another-interceptor :something)]

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor third-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor first-interceptor third-interceptor]})))

      (is (= {:io.pedestal.http/interceptors [first-interceptor second-interceptor third-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor third-interceptor first-interceptor]})))

      (is (= {:io.pedestal.http/interceptors [first-interceptor third-interceptor second-interceptor]}
             (angel/satisfy {:io.pedestal.http/interceptors [third-interceptor second-interceptor first-interceptor]})))))

  (testing "blows up if dependency can't be satisfied"
    (let [first-interceptor (angel/provides some-interceptor :something)
          second-interceptor (angel/requires another-interceptor :something-else)]

      (is (thrown-with-msg?
           Exception #"No interceptor provides :something-else to satisfy :angel.interceptor-test/another-interceptor"
           (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor first-interceptor]})))))

  (comment "todo"
           (testing "blows up if there is a dependency loop"
             (let [first-interceptor (angel/requires (angel/provides some-interceptor :something) :something-else)
                   second-interceptor (angel/requires (angel/provides another-interceptor :something-else) :something)]

               (is (thrown-with-msg?
                    Exception #"Requires/provides loop between \[:angel.interceptor-test/some-interceptor :angel.interceptor-test/another-interceptor\]"
                    (angel/satisfy {:io.pedestal.http/interceptors [second-interceptor first-interceptor]})))))))


(def first-interceptor (angel/provides another-interceptor :another-thing))
(def second-interceptor (angel/requires some-interceptor :another-thing))
(def handler (pedestal-interceptor/interceptor something))

(defroutes routes
  [[["/something" ^:interceptors [second-interceptor]
     ["/another" ^:interceptors [first-interceptor]
      {:get handler}]]]])

(deftest satisfy-routes-test

  (testing "reorders interceptors within routes to put requires after the appropriate provides"

    (is (= [first-interceptor
            second-interceptor]

           (->> (angel/satisfy {:io.pedestal.http/routes routes})
                :io.pedestal.http/routes first :interceptors (take 2))))))
