#!/usr/bin/bash

if [ -f "docs-server-vertx.jar" ]; then
  java -jar docs-server-vertx.jar --conf=config-prod.json --options=vertx-options.json &
fi
if [ -f "docs-server-vertx-fat.jar" ]; then
  java -jar docs-server-vertx-fat.jar --conf=config-prod.json --options=vertx-options.json &
fi
