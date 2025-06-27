#!/usr/bin/bash

ps aux | grep docs-server-vertx.jar | head -n 1 | awk '{print $2}' | xargs kill -9
