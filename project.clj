(defproject me.dryewo/clj-github-app "0.1.1"
  :description "A library to implement GitHub Apps in Clojure."
  :url "http://github.com/dryewo/clj-github-app"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[clj-http "3.9.1"]
                 [org.clojure/core.cache "0.7.1"]
                 [com.auth0/java-jwt "3.4.1"]
                 [org.bouncycastle/bcprov-jdk15on "1.60"]
                 [org.bouncycastle/bcpkix-jdk15on "1.60"]
                 [pandect "0.6.1"]
                 [ring/ring-codec "1.1.1"]
                 [crypto-equality "1.0.0"]
                 [org.bovinegenius/exploding-fish "0.3.6"]]
  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"update-readme-version" ["shell" "sed" "-i" "s/\\\\[me\\.dryewo\\\\/clj-github-app \"[0-9.]*\"\\\\]/[me\\.dryewo\\\\/clj-github-app \"${:version}\"]/" "README.md"]}
  :release-tasks [["shell" "git" "diff" "--exit-code"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["changelog" "release"]
                  ["update-readme-version"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["vcs" "push"]])
