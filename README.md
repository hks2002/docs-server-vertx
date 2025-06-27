# Docs Server Vertx

![Github Version](https://img.shields.io/github/v/release/hks2002/docs-server-vertx?display_name=release)
![Github Build Status](https://img.shields.io/github/actions/workflow/status/hks2002/docs-server-vertx/Build-Test-Release.yml)
![GitHub License](https://img.shields.io/github/license/hks2002/docs-server-vertx)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![release-please: angular](https://img.shields.io/badge/release--please-angular-e10079?style=flat&logo=google)](https://github.com/google-github-actions/release-please-action)

The docs server developed with vertx.

#### Vertx Help
* https://vertx.io/docs/ [Vert.x Documentation]
* https://vertx-china.github.io/ [Vert.x 中文文档]

#### How to deploy it
1. using fat version, run ```java -jar docs-server-vertx-fat.jar```.
2. or using thin version, copy `libs` and `docs-server-vertx.jar` into same folder, then run ```java -jar docs-server-vertx.jar```.
3. passing vterx options, run  ```java -jar docs-server-vertx.jar --options=options.json```.
3. passing config, run ```java -jar docs-server-vertx.jar --conf=config-prod.json```.

   > Allow the TLS disabled algorithms (As Required)
   > If the connect database version is too old, and the running Linux system is new, you maybe will have the TLS connection issue by disabled algorithms.

   > Edit`JAVA_HOME/conf/security/java.security`, Delete `dk.tls.disabledAlgorithms`disabled algorithms value；
   > Edit`/etc/crypto-policies/back-ends/java.config`, Delete `jdk.tls.disabledAlgorithms`disabled algorithms value；   