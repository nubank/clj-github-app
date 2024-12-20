(defproject nubank/clj-github-app "0.3.0"
  :description "A library to implement GitHub Apps in Clojure."
  :url "http://github.com/nubank/clj-github-app"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["publish" {:url "https://clojars.org/repo"
                             :username :env/clojars_username
                             :password :env/clojars_passwd
                             :sign-releases false}]]
  :dependencies [[cheshire/cheshire "5.13.0"]
                 [clj-http/clj-http "3.13.0"]
                 [org.clojure/core.cache "1.1.234"]
                 [com.auth0/java-jwt "4.4.0"]
                 [org.bouncycastle/bcpkix-jdk18on "1.79"]
                 [org.bouncycastle/bcprov-jdk18on "1.79"]
                 [pandect/pandect "1.0.2"]
                 [crypto-equality/crypto-equality "1.0.1"]]
  :plugins [[lein-ancient "0.7.0"]
            [lein-changelog "0.3.2"]
            [lein-cljfmt "0.9.2" :exclusions [org.clojure/clojure]]
            [lein-cloverage "1.2.4"]
            [lein-nsorg "0.3.0" :exclusions [org.clojure/clojure]]
            [lein-shell "0.5.0"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.12.0"]]}}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "s|\\\\[nubank/clj-github-app \"[0-9.]*\"\\\\]|[nubank/clj-github-app \"${:version}\"]|" "README.md"]
            "lint"                  ["do" ["cljfmt" "check"] ["nsorg"]]
            "lint-fix"              ["do" ["cljfmt" "fix"] ["nsorg" "--replace"]]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
