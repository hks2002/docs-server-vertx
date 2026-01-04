#!/usr/bin/bash

./shutdown.sh
java -jar docs-server-vertx.jar com.da.docs.WebServerVerticle --conf=config-prod.json --options=vertx-options.json
