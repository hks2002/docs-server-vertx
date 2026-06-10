#!/usr/bin/bash

ps aux | grep docs-server-vertx-fat.jar | awk 'NR==1{print $2}' | xargs -r kill -9
