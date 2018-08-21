(ns clj-github-app.client
  (:require [clj-http.conn-mgr :as conn-mgr]
            [clj-github-app.token-manager :as token-manager]
            [clj-http.client :as http]
            [org.bovinegenius.exploding-fish :as uri]
            [clojure.string :as str])
  (:import (java.lang AutoCloseable)))


(defprotocol AppClient
  (app-request* [_ opts])
  (app-request [_ method url opts])
  (request* [_ installation-id opts])
  (request [_ installation-id method url opts]))


(defn request-impl [connection-pool token opts]
  (http/request
    (merge {:oauth-token        token
            :connection-manager connection-pool
            :as                 :json}
           opts)))


(defn remove-leading-slash [url-or-path]
  (let [trimmed-url-or-path (str/trim url-or-path)]
    (if (= \/ (first trimmed-url-or-path))
      (subs trimmed-url-or-path 1)
      trimmed-url-or-path)))


(defn resolve-url [path-or-url github-api-url]
  (->> path-or-url
       remove-leading-slash
       (uri/resolve-uri (str github-api-url "/"))))


(defrecord AppClientImpl [github-api-url token-manager connection-pool]
  AppClient

  (app-request* [_ opts]
    (let [app-token (token-manager/get-app-token token-manager)]
      (request-impl connection-pool app-token (update opts :url resolve-url github-api-url))))

  (app-request [this method path-or-url opts]
    (app-request* this (merge {:method method :url path-or-url} opts)))

  (request* [_ installation-id opts]
    (let [installation-token (token-manager/get-installation-token token-manager installation-id)]
      (request-impl connection-pool installation-token (update opts :url resolve-url github-api-url))))

  (request [this installation-id method path-or-url opts]
    (request* this installation-id (merge {:method method :url path-or-url} opts)))

  AutoCloseable
  (close [_]
    (conn-mgr/shutdown-manager connection-pool)))


(defn make-app-client [github-api-url github-app-id private-key-pem-str connection-pool-opts]
  (AppClientImpl.
    github-api-url
    (token-manager/make-token-manager github-api-url github-app-id private-key-pem-str)
    (conn-mgr/make-reusable-conn-manager connection-pool-opts)))
