#!/usr/bin/bash

./shutdown-fat.sh
java -jar docs-server-vertx-fat.jar com.da.docs.WebServerVerticle --conf=config-prod.json --options=vertx-options.json

