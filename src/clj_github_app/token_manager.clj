(ns clj-github-app.token-manager
  (:require [clj-http.client :as http]
            [clojure.core.cache :as cache])
  (:import (clojure.core.cache CacheProtocol)
           (com.auth0.jwt JWT)
           (com.auth0.jwt.algorithms Algorithm)
           (java.io StringReader)
           (java.net URI URLEncoder)
           (java.nio.charset StandardCharsets)
           (java.security KeyFactory)
           (java.security.spec PKCS8EncodedKeySpec)
           (java.text SimpleDateFormat)
           (java.util Date)
           (org.bouncycastle.jce.provider BouncyCastleProvider)
           (org.bouncycastle.openssl PEMParser)))


;; See make-token-manager function below


(defprotocol GitHubTokenManager
  (get-app-token [token-manager])
  (get-installation-token [token-manager installation-id]))

(defn- make-signing-algorithm [pem-str]
  (let [private-key (->> pem-str
                         (StringReader.)
                         (PEMParser.)
                         (.readPemObject)
                         (.getContent)
                         (PKCS8EncodedKeySpec.)
                         (.generatePrivate (KeyFactory/getInstance "RSA" (BouncyCastleProvider.))))]
    ;; Only private key given (only for signing, not for verifying)
    (Algorithm/RSA256 nil private-key)))

(defn- make-app-token [signing-algorithm app-id]
  ;; If you get: 'Issued at' claim ('iat') must be an Integer representing the time that the assertion was issued
  ;;   this means issuedAt is in the future according to GitHub's clock
  (let [now     (Date.)
        now-30s (Date. ^long (-> now (.getTime) (- (* 1000 30))))
        now+8m  (Date. ^long (-> now (.getTime) (+ (* 1000 60 8))))]
    (-> (JWT/create)
        (.withIssuer (str app-id))
        (.withIssuedAt now-30s)
        (.withExpiresAt now+8m)
        (.sign signing-algorithm))))

(cache/defcache GithubAppTokenCache [cache]
  CacheProtocol
  (lookup [this item]
          (let [ret (cache/lookup this item ::nope)]
            (when-not (= ::nope ret) ret)))
  (lookup [this item not-found]
          (if (cache/has? this item)
            (get cache item)
            not-found))
  (has? [this item]
        (let [now (Date.)
              {:keys [expires-at-parsed]} (get cache item)]
          (when expires-at-parsed
            (.before now expires-at-parsed))))
  (hit [this item] this)
  (miss [this item result]
        (let [now               (Date.)
              expires-at-parsed (try
                                  (.parse (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ssX") (:expires_at result))
                                  (catch Exception _
                                    now))]
          (GithubAppTokenCache. (assoc cache item (assoc result :expires-at-parsed expires-at-parsed)))))
  (seed [_ base]
        (GithubAppTokenCache. base))
  (evict [_ item]
         (GithubAppTokenCache. (dissoc cache item)))
  Object
  (toString [_]
            (str cache)))

(defrecord GitHubTokenManagerImpl [cache get-app-token-fn get-installation-token-fn]
  GitHubTokenManager
  (get-app-token [_]
    (get-app-token-fn))
  (get-installation-token [_ installation-id]
    (:token (cache/lookup (swap! cache cache/through-cache installation-id get-installation-token-fn)
                          installation-id))))

(defn- url-encode [^String s]
  (URLEncoder/encode s StandardCharsets/UTF_8))

(defn- installation-token-uri
  [github-api-url installation-id]
  (-> (URI/create (str github-api-url "/"))
      (.resolve (str "app/installations/" (url-encode (str installation-id)) "/access_tokens"))
      .normalize
      .toString))

(defn make-token-manager [github-api-url github-app-id private-key-pem-str]
  (let [signing-algorithm (make-signing-algorithm private-key-pem-str)
        cache             (atom (GithubAppTokenCache. {}))]
    (GitHubTokenManagerImpl.
     cache
     (fn []
       (make-app-token signing-algorithm github-app-id))
     (fn [installation-id]
       (let [url (installation-token-uri github-api-url installation-id)]
         (:body (http/post url
                           {:oauth-token (make-app-token signing-algorithm github-app-id)
                            :as          :json
                            :accept      "application/vnd.github.machine-man-preview+json"})))))))
