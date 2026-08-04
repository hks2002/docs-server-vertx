#!/usr/bin/bash

./shutdown.sh
java -cp "docs-server-vertx.jar" com.da.docs.VertxApp com.da.docs.WebServerVerticle --conf=config-prod.json --options=vertx-options.json
