(ns clj-github-app.utils
  (:require [clj-github-app.httpkit-client :as client]))

(defn fetch-body! [client request]
  (:body (client/request client request)))

