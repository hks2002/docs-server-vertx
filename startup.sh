#!/usr/bin/bash

./shutdown.sh
java -jar docs-server-vertx.jar --conf=config-prod.json --options=vertx-options.json
