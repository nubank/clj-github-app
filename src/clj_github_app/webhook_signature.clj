(ns clj-github-app.webhook-signature
  (:require [pandect.algo.sha1 :as sha1]
            [crypto.equality]
            [clojure.string :as str]))


(defn check-payload-signature [webhook-secret x-hub-signature payload]
  (if (str/blank? webhook-secret)
    ::not-checked
    (if (str/blank? x-hub-signature)
      ::missing-signature
      (let [payload-signature (str "sha1=" (sha1/sha1-hmac payload webhook-secret))]
        (if-not (crypto.equality/eq? payload-signature x-hub-signature)
          ::wrong-signature
          ::ok)))))


