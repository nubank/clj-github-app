# clj-github-app
[![Build Status](https://travis-ci.org/dryewo/clj-github-app.svg?branch=master)](https://travis-ci.org/dryewo/clj-github-app)
[![codecov](https://codecov.io/gh/dryewo/clj-github-app/branch/master/graph/badge.svg)](https://codecov.io/gh/dryewo/clj-github-app)
[![Clojars Project](https://img.shields.io/clojars/v/me.dryewo/clj-github-app.svg)](https://clojars.org/me.dryewo/clj-github-app)

A library to implement [GitHub Apps] in Clojure.

```clj
[me.dryewo/clj-github-app "0.0.0"]
```

Includes:

* [Webhook payload signature checker][webhook-signatures] with [secure comparison](https://github.com/weavejester/crypto-equality).
* API client with HTTP connection pool.
* Access token manager with caching ([Authenticating with GitHub Apps] is tricky).

## Usage

### Checking webhook signatures

When implementing a webhook handler, it is recommended to check the webhook request signature before processing it.
Please read the [official documentation][webhook-signatures] first.

Imagine you have a webhook handler:

```clj
(ns your-project.webhooks
  (:require [clj-github-app.webhook-signature :as webhook-signature]))

(def GITHUB_WEBHOOK_SECRET (System/getenv "GITHUB_WEBHOOK_SECRET"))

(defn post-github "Checks if the webhook is valid and handles it." [request]
  (let [{:strs [x-github-delivery x-github-event x-hub-signature]} (:headers request)
        payload (slurp (:body request))]
    (case (webhook-signature/check-payload-signature GITHUB_WEBHOOK_SECRET x-hub-signature payload)
      ::webhook-signature/missing-signature {:status 400 :body "x-hub-signature header is missing"}
      ::webhook-signature/wrong-signature {:status 401 :body "x-hub-signature does not match"}
      (let [parsed-payload  (json/parse-string payload keyword)]
        ;; process your webhook here
        {:status 200 :body "This is fine."}))))
```

The key part here is the call to  `check-payload-signature`. It takes 3 arguments:

* `webhook-secret` — the exact secret string that you set when configuring webhook for your repo.  
    If this argument is blank or nil, `check-payload-signature` will do nothing and return
    `:clj-github-app.webhook-signature/not-checked`.
* `x-hub-signature` — contents of "X-Hub-Signature" request header.
* `payload` — request body as a string.

Possible return values:

* `:clj-github-app.webhook-signature/ok` — signature matches the payload.
* `:clj-github-app.webhook-signature/wrong-signature` — signature does not match the payload.
* `:clj-github-app.webhook-signature/missing-signature` — `x-hub-signature` parameter was blank or nil.
* `:clj-github-app.webhook-signature/not-checked` — no check was done because `webhook-secret` parameter was blank or nil.


### Authenticating as a GitHub App

Please read [Authenticating with GitHub Apps] official documentation first.

Example (uses [mount-lite]):

```clj
(ns your-project.external.github
  (:require [mount.lite :as m]
            [clj-github-app.client :as client]))

(def GITHUB_API_URL "https://api.github.com")
(def GITHUB_APP_ID (System/getenv "GITHUB_APP_ID"))
(def GITHUB_APP_PRIVATE_KEY_PEM (System/getenv "GITHUB_APP_PRIVATE_KEY_PEM"))

(m/defstate client
  :start (client/make-app-client GITHUB_API_URL GITHUB_APP_ID GITHUB_APP_PRIVATE_KEY_PEM {})
  :stop (.close @client))
```

`clj-github-app.client/make-app-client` takes 4 parameters:

* `github-api-url` — Base URL of GitHub API. Usually `https://api.github.com` or something like `https://github.example.com/api/v3` for GHE.
* `github-app-id` — GitHub App ID as string (can be found on the app settings page).
* `private-key-pem-str` — String contents of the private key file that you [generated when configuring the app](https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#generating-a-private-key).
* `connection-pool-opts` — [clj-http connection pool parameters](https://github.com/dakrone/clj-http#persistent-connections).
    Can be set to `{}` to use all defaults.

It returns an object that implements `AutoCloseable` interface and `AppClient` protocol, which has the following functions:

* `request*` — to [authenticate as an installation][as-installation].
    Given `installation-id` and `opts`, makes an HTTP request to GitHub API, automatically retrieving an access token.  
    Uses [clj-http], `opts` argument is given to `request` function as described [here](https://github.com/dakrone/clj-http#raw-request).
    `opts` is supposed to include `:method` and `:url` keys.
    This function is the main workhorse.
* `request` — same as `request*`, but has separate arguments for method and URL.
* `app-request*` — to [authenticate as a GitHub App][as-app].
    This is only useful for querying app metadata.
* `app-request` — same as `app-request*`, but has separate arguments for method and URL.

You can [authenticate as a GitHub App][as-app]:

```clj
(client/app-request* @client {:method :get :url "/app" :accept "application/vnd.github.machine-man-preview+json"})
(client/app-request @client :get "/app" {:accept "application/vnd.github.machine-man-preview+json"})
```

You can also [authenticate as an installation][as-installation]. For this you need Installation ID,
(which is usually given to you in webhook payloads):

```clj
(client/request* @client 42 {:method :get :url "/repos/myname/myrepo/issues/123/comments")
(client/request @client 42 :get "/repos/myname/myrepo/issues/123/comments" {})
```

All these functions can accept either a full URL or just a relative path, which will be automatically appended to the base
GitHub API URL, given earlier to `make-app-client`.  
The "path only" mode is useful when you are constructing the URL yourself and don't want to repeat the base API URL there.
The path can start with a `/` or not, which makes no difference, both cases are handled the same way.
The "full URL" mode is useful when you use a URL extracted from a webhook payload
and don't want to strip the base URL part from there.

```clj
;; Use github-api-url (provided earlier to make-app-client) as base API URL
(client/app-request @client :get "foo" {})
(client/app-request @client :get "/foo" {})
;; The same call, but without relying on github-api-url
(client/app-request @client :get "https://api.github.com/foo" {})
```

#### Convenience wrappers for API endpoints

This library does not provide any wrappers like

```clj
(list-issue-comments "owner" "repo" "123" {:since "2018-01-01"})
```

Such wrappers are really easy to implement on your own:

```clj
(defn create-list-issue-comments-request [owner repo issue-number params]
  {:method       :get
   :url          (format "/repos/%s/%s/issues/%s/comments" owner repo issue-number)
   :query-params params})
```

and then use like this:

```clj
(client/request @client 42 (create-list-issue-comments-request "owner" "repo" "123" {:since "2018-01-01"}))
```

Full GitHub API reference can be found [here](https://developer.github.com/v3/).

## Development

With every commit, add important changes from it to the "Unreleased" section of _CHANGELOG.md_.

### Release procedure

Before releasing:

1. Commit all changes.
2. Add a section for the upcoming version to _CHANGELOG.md_, move stuff from then "Unreleased" section there, link the version.
3. Update `[Unreleased]` link in the end of the file 
4. Commit the changes to _CHANGELOG.md_.
5. Run `lein release` as described below.

TODO automate this changelog work. 

Library version will be updated in _project.clj_ and _README.md_ automatically after calling `lein release`.

    lein release :patch
    # or
    lein release :minor
    # or
    lein release :major

## License

Copyright © 2018 Dmitrii Balakhonskii

Distributed under the Eclipse Public License version 1.0.


[GitHub Apps]: https://developer.github.com/apps/about-apps/#about-github-apps
[Authenticating with GitHub Apps]: https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/
[webhook-signatures]: https://developer.github.com/webhooks/securing/#validating-payloads-from-github
[as-app]: https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#accessing-api-endpoints-as-a-github-app
[as-installation]: https://developer.github.com/apps/building-github-apps/authenticating-with-github-apps/#accessing-api-endpoints-as-an-installation
[clj-http]: https://github.com/dakrone/clj-http
[mount-lite]: https://github.com/aroemers/mount-lite
