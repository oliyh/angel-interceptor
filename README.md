# angel-interceptor

Express relations between Pedestal interceptors - decouple the declaration of interceptors on route definitions from execution order.

```clojure
(require '[angel.interceptor :as ai])

["/api" ^:interceptors [(ai/interceptor rate-limiter
                                        :requires [:account])]
  ["/slack" ^:interceptors [(ai/interceptor slack-auth
                                            :provides [:account])] ...]
  ["/hipchat" ^:interceptors [(ai/interceptor hipchat-auth
                                              :provides [:account])] ...]]
```

`rate-limiter` will run *after* `slack-auth` or `hipchat-auth` but still run *before* the handler.

## Introduction

Pedestal interceptors are applied sequentially in the order in which they are specified, starting with
those defined at the service level and progressing on through those defined at route branch and finally route leaf level.

This can prevent reusable interceptors or require repetition, or require injection of interceptors at run time.
Angel Interceptor allows you to express relations between interceptors to gain maximum reuse without repetition.

## Explanation

Imagine you are building an API for integration with multiple chat services.
You would naturally put `slack-auth` and `hipchat-auth` as interceptors on the appropriate route branches, as below.

```clojure
["/api" ^:interceptors [rate-limiter]
  ["/slack" ^:interceptors [slack-auth] ...]
  ["/hipchat" ^:interceptors [hipchat-auth] ...]]
```

You want to limit the number of times each account can use your API, so you add an interceptor to do this - `rate-limiter` - which applies to all routes in the API.
This interceptor requires an `account-id` so it can work out how many times it has already been accessed to decide whether to reject the request or not.
You want to run it *before* the handler so that you don't needlessly perform mutating or expensive operations.

The problem here is that in Pedestal the `rate-limiter` interceptor will run *before* the `slack-auth` or `hipchat-auth` interceptors, so the account-id will not be available.

Angel Interceptor allows you to express relations between interceptors to allow you to solve this problem.

```clojure
(require '[angel.interceptor :as ai])

["/api" ^:interceptors [(ai/interceptor rate-limiter
                                        :requires [:account])]
  ["/slack" ^:interceptors [(ai/interceptor slack-auth
                                            :provides [:account])] ...]
  ["/hipchat" ^:interceptors [(ai/interceptor hipchat-auth
                                              :provides [:account])] ...]]
```

Angel Interceptor will then reorder your interceptors such that `rate-limiter` will run immediately after `slack-auth` or `hipchat-auth`.

## Bugs

Please raise issues or send pull requests on GitHub

## License

Copyright © 2015 Oliver Hine

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
