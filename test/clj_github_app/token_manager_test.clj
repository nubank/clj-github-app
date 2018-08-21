(ns clj-github-app.token-manager-test
  (:require [clojure.test :refer :all]
            [clj-github-app.token-manager :refer :all :as tm]
            [clj-http.client :as http])
  (:import (com.auth0.jwt JWT)))


(def access-token-response {:body {:token      "installation-token"
                                   :expires_at "2100-07-11T22:14:10Z"}})


(defn make-test-token-manager []
  (make-token-manager "https://github-api.example.com" "app-id" (slurp "test/example-private-key.pem")))


(deftest works
  (testing "App token is a valid JWT with App ID included as issuer"
    (let [tm (make-test-token-manager)]
      (is (= "app-id" (-> (get-app-token tm) JWT/decode bean :issuer)))))

  (testing "Installation token is retrieved correctly"
    (with-redefs [tm/make-app-token (fn [_ _] "app-token")
                  http/post         (fn [url opts]
                                      (is (= url "https://github-api.example.com/installations/1/access_tokens"))
                                      (is (= (:oauth-token opts) "app-token"))
                                      access-token-response)]
      (let [tm (make-test-token-manager)]
        (is (= "installation-token" (get-installation-token tm "1"))))))

  (testing "Installation token is cached"
    (let [tm           (make-test-token-manager)
          times-called (atom 0)]
      (with-redefs [tm/make-app-token (fn [_ _] "app-token")
                    http/post         (fn [_ _]
                                        (swap! times-called inc)
                                        access-token-response)]
        (get-installation-token tm "1")
        (get-installation-token tm "1")
        (is (= 1 @times-called))))))
