(ns clj-github-app.webhook-signature
  (:require [clojure.string :as str]
            [crypto.equality]
            [pandect.algo.sha1 :as sha1]
            [pandect.algo.sha256 :as sha256]))

(defn ^{:deprecated "Prefer using check-payload-signature-256."} check-payload-signature [webhook-secret x-hub-signature payload]
  (if (str/blank? webhook-secret)
    ::not-checked
    (if (str/blank? x-hub-signature)
      ::missing-signature
      (let [payload-signature (str "sha1=" (sha1/sha1-hmac payload webhook-secret))]
        (if-not (crypto.equality/eq? payload-signature x-hub-signature)
          ::wrong-signature
          ::ok)))))

(defn check-payload-signature-256 [webhook-secret x-hub-signature-256 payload]
  (if (str/blank? webhook-secret)
    ::not-checked
    (if (str/blank? x-hub-signature-256)
      ::missing-signature
      (let [payload-signature (str "sha256=" (sha256/sha256-hmac payload webhook-secret))]
        (if-not (crypto.equality/eq? payload-signature x-hub-signature-256)
          ::wrong-signature
          ::ok)))))
