(ns clj-github-app.client-test
  (:require [clj-github-app.client :as client]
            [clj-http.client :as http]
            [clojure.test :refer :all]))

(defn make-test-client []
  (client/make-app-client "https://github.example.com/api/v3" "app-id" (slurp "test/example-private-key.pem") {}))

(def access-token-response {:body {:token      "installation-token"
                                   :expires_at "2100-07-11T22:14:10Z"}})

(deftest about-url-passing
  (testing "User is able to provide both full URL or just a path, which is resolved against the configured base API URL"

    (testing "with app-request and app-request*"
      (are [?path-or-url ?resulting-url]
           (let [resulting-url (atom nil)]
             (with-redefs [client/request-impl (fn [_ _ opts]
                                                 (reset! resulting-url (:url opts)))]
               (with-open [c (make-test-client)]
                 (client/app-request c :get ?path-or-url {})
                 (client/app-request* c {:method :get :url ?path-or-url})))
             (= ?resulting-url @resulting-url))
        "foo" "https://github.example.com/api/v3/foo"
        "https://github.example.com/api/v3/bar" "https://github.example.com/api/v3/bar"
        "https://api.github.com/bar" "https://api.github.com/bar"))

    (testing "with request and request*"
      (are [?path-or-url ?resulting-url]
           (let [resulting-url (atom nil)]
             (with-redefs [http/post           (fn [_ _]
                                                 access-token-response)
                           client/request-impl (fn [_ _ opts]
                                                 (reset! resulting-url (:url opts)))]
               (with-open [c (make-test-client)]
                 (client/request c "inst-id" :get ?path-or-url {})
                 (client/request* c "inst-id" {:method :get :url ?path-or-url})))
             (= ?resulting-url @resulting-url))
        "foo" "https://github.example.com/api/v3/foo"
        "https://github.example.com/api/v3/bar" "https://github.example.com/api/v3/bar"
        "https://api.github.com/bar" "https://api.github.com/bar"))))

(deftest about-resolve-url
  (testing "Handles correctly all slash combinations"
    (are [?github-api-url ?path-or-url]
         (= "https://github.example.com/api/v3/foo/bar" (client/resolve-url ?path-or-url ?github-api-url))
      "https://github.example.com/api/v3" "foo/bar"
      "https://github.example.com/api/v3/" "foo/bar"
      "https://github.example.com/api/v3" "/foo/bar"
      "https://github.example.com/api/v3/" "/foo/bar")))
