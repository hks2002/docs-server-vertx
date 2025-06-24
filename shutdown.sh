#!/usr/bin/bash

ps aux | grep docs-server-vertx.jar | head -n 1 | awk '{print $2}' | xargs kill -9
ps aux | grep docs-server-vertx-fat.jar | head -n 1 | awk '{print $2}' | xargs kill -9
