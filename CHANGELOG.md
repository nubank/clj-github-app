# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.0] — 2024-12-06
### Changed
* Upgrade dependencies
  * cheshire from 5.11.0 to 5.13.0
  * clj-http from 3.12.3 to 3.13.0
  * org.clojure/core.cache from 1.0.225 to 1.1.234
  * com.auth0/java-jwt from 4.0.0 to 4.4.0
  * org.bouncycastle/* from 1.78.1 to 1.79
* Remove some dependencies
  * ring/ring-codec
  * org.bovinegenius/exploding-fish

## [0.2.2] — 2024-08-07
### Changed
* Bump dependencies

## [0.2.1] — 2024-08-06

## [0.2.0] — 2022-10-03

## [0.1.4] — 2021-03-10
### Changed
* Update github url to address deprecation

### Added
* Include cheshire as a project dependency

## [0.1.3] — 2019-01-23
### Changed
* Bump library versions

## [0.1.2] — 2018-11-11
### Changed
* Bump library versions

## [0.1.1] — 2018-09-03
### Added
* Development-related:
  * Use lein-ancient in CI to prevent outdated dependencies
  * Use lein-changelog to automate changelog tasks

## 0.1.0 — 2018-08-21
### Added
* Webhook payload signature checker with secure comparison.
* API client with HTTP connection pool.
* Access token manager with caching

[0.1.1]: https://github.com/nubank/clj-github-app/compare/0.1.0...0.1.1
[0.1.2]: https://github.com/nubank/clj-github-app/compare/0.1.1...0.1.2
[0.1.3]: https://github.com/nubank/clj-github-app/compare/0.1.2...0.1.3
[0.1.4]: https://github.com/nubank/clj-github-app/compare/0.1.3...0.1.4
[0.2.0]: https://github.com/nubank/clj-github-app/compare/0.1.4...0.2.0
[0.2.1]: https://github.com/nubank/clj-github-app/compare/0.2.0...0.2.1
[0.2.2]: https://github.com/nubank/clj-github-app/compare/0.2.1...0.2.2
[Unreleased]: https://github.com/nubank/clj-github-app/compare/0.2.2...HEAD
