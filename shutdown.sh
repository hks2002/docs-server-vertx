#!/usr/bin/bash

ps aux | grep docs-server-vertx.jar | awk 'NR==1{print $2}' | xargs -r kill -9
