#!/usr/bin/bash

./shutdown-fat.sh
java -jar docs-server-vertx-fat.jar --conf=config-prod.json --options=vertx-options.json

