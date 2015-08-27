# angel-interceptor

Express relations between Pedestal interceptors and decouple the scope of interceptors from execution order.

```clojure
(require '[angel.interceptor :as ai])

["/api" ^:interceptors [(ai/interceptor rate-limiter :requires [:account])]
  ["/slack" ^:interceptors [(ai/interceptor slack-auth :provides [:account])] ...]
  ["/hipchat" ^:interceptors [(ai/interceptor hipchat-auth :provides [:account])] ...]]
```

`rate-limiter` will run *after* `slack-auth` or `hipchat-auth` but still run *before* the handler.

## Introduction

Pedestal interceptors are applied sequentially in the order in which they are specified, starting with
those defined at the service level and progressing on through those defined at route branch and finally route leaf level.

_Angel Interceptor_ allows you to express relations between interceptors to gain maximum reuse without repetition.

## Use case

Imagine you are building an API for integration with multiple chat services.
You would naturally put `slack-auth` and `hipchat-auth` as interceptors on the appropriate route branches, as below.

```clojure
["/api" ^:interceptors [rate-limiter]
  ["/slack" ^:interceptors [slack-auth] ...]
  ["/hipchat" ^:interceptors [hipchat-auth] ...]]
```

You want to limit the number of times each account can use your API, so you add an interceptor - `rate-limiter` - which applies to all routes in the API.
It requires an `account-id` to see if the account has exceeded its limit and runs *before* the handler to avoid performing mutating or expensive operations.

In Pedestal the `rate-limiter` interceptor will run *before* the `slack-auth` or `hipchat-auth` interceptors, so the `account-id` will not be available.

_Angel Interceptor_ allows you to express a _provides_ and _requires_ relation between interceptors:

```clojure
(require '[angel.interceptor :as ai])

["/api" ^:interceptors [(ai/interceptor rate-limiter :requires [:account])]
  ["/slack" ^:interceptors [(ai/interceptor slack-auth :provides [:account])] ...]
  ["/hipchat" ^:interceptors [(ai/interceptor hipchat-auth :provides [:account])] ...]]
```

_Angel Interceptor_ will then reorder your interceptors such that `rate-limiter` will run immediately after `slack-auth` or `hipchat-auth`.

## Bugs

Please raise issues or send pull requests on GitHub

## License

Copyright © 2015 Oliver Hine

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
