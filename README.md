# angel-interceptor

[![Clojars Project](http://clojars.org/angel-interceptor/latest-version.svg)](http://clojars.org/angel-interceptor)

Express relations between Pedestal interceptors and decouple scope from execution order.
Include or exclude interceptors based on predicates.

Requires Pedestal 0.5.0 or greater.

```clojure
(require '[angel.interceptor :as angel]
         '[io.pedestal.http :as bootstrap]
         '[io.pedestal.http.route.definition :refer [defroutes]])

(defn- not-prod? []
  (not= :prod (:env (get-settings))))

(defroutes routes
  ["/api" ^:interceptors [(angel/requires rate-limiter :account)
                          (angel/conditional show-stacktraces not-prod?)]
    ["/slack" ^:interceptors [(angel/provides slack-auth :account)] ...]
    ["/hipchat" ^:interceptors [(angel/provides hipchat-auth :account)] ...]])

(def service
  (-> {::bootstrap/routes routes}
      angel/satisfy
      bootstrap/default-interceptors))
```

`rate-limiter` will run *after* `slack-auth` or `hipchat-auth` but still run *before* the handler.

## Introduction

Pedestal interceptors are applied sequentially in the order in which they are specified, starting with
those defined at the service level and progressing on through those defined at route branch and finally route leaf level.

**Angel Interceptor** allows you to express relations between interceptors and conditions of inclusion
to gain maximum reuse without repetition.

## Dependent Interceptors

Imagine you are building an API for integration with multiple chat services.
You would naturally put `slack-auth` and `hipchat-auth` as interceptors on the appropriate route branches, as below.

```clojure
(require '[io.pedestal.http.route.definition :refer [defroutes]])

(defroutes routes
  ["/api" ^:interceptors [rate-limiter]
    ["/slack" ^:interceptors [slack-auth] ...]
    ["/hipchat" ^:interceptors [hipchat-auth] ...]])
```

You want to limit the number of times each account can use your API, so you add an interceptor - `rate-limiter` - which applies to all routes in the API.
It requires an `account-id` to see if the account has exceeded its limit and runs *before* the handler to avoid performing mutating or expensive operations.

In Pedestal the `rate-limiter` interceptor will run *before* the `slack-auth` or `hipchat-auth` interceptors, so the `account-id` will not be available.

**Angel Interceptor** allows you to express a _provides_ and _requires_ relation between interceptors:

```clojure
(require '[angel.interceptor :as angel]
         '[io.pedestal.http.route.definition :refer [defroutes]])

(defroutes routes
  ["/api" ^:interceptors [(angel/requires rate-limiter :account)]
    ["/slack" ^:interceptors [(angel/provides slack-auth :account)] ...]
    ["/hipchat" ^:interceptors [(angel/provides hipchat-auth :account)] ...]])
```

**Angel Interceptor** will then reorder your interceptors such that `rate-limiter` will run immediately after `slack-auth` or `hipchat-auth`.
To do this, simply run `angel/satisfy` over the service map:

```clojure
(def service
  (angel/satisfy
    {:io.pedestal.http/routes routes}))
```

## Conditional Interceptors

Sometimes interceptors may apply, or not apply, depending on some external factor e.g. you are running in a non-production
environment. **Angel Interceptor** allows you to express predicates for including interceptors:

```clojure
(require '[angel.interceptor :as angel]
         '[io.pedestal.http.route.definition :refer [defroutes]])

(defn- not-prod? []
  (not= :prod (:env (get-settings))))

(defroutes routes
  ["/api" ^:interceptors [(angel/conditional show-stacktraces not-prod?)]
    ["/slack" ...]
    ["/hipchat" ...]])
```

If the conditional evaluates to `false` the interceptor is removed from the chain when `angel/satisfy` is run.

## Build
[![CircleCI](https://circleci.com/gh/oliyh/angel-interceptor.svg?style=svg)](https://circleci.com/gh/oliyh/angel-interceptor)

## Bugs

Please raise issues or send pull requests on GitHub

## License

Copyright © 2015 Oliver Hine

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
