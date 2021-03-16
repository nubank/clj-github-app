(ns clj-github-app.webhook-signature-test
  (:require [clj-github-app.webhook-signature :refer :all :as ws]
            [clojure.test :refer :all]
            [pandect.algo.sha1 :as sha1]))

(deftest works
  (testing "When webhook secret is blank, returns :clj-github-app.webhook-signature/not-checked"
    (are [?in]
         (= ::ws/not-checked (check-payload-signature ?in nil nil))
      nil
      ""
      " "))

  (testing "When X-Hub-Signature is blank or missing, returns :clj-github-app.webhook-signature/missing-signature"
    (are [?in]
         (= ::ws/missing-signature (check-payload-signature "secret" ?in nil))
      nil
      ""
      " "))

  (testing ""
    (are [?payload ?res]
         (= ?res (check-payload-signature "secret" (str "sha1=" (sha1/sha1-hmac "signed-payload" "secret")) ?payload))
      "signed-payload" :clj-github-app.webhook-signature/ok
      "tampered-payload" :clj-github-app.webhook-signature/wrong-signature)))
