if [ -f "docs-server-vertx.jar" ]; then
  java -jar docs-server-vertx.jar &
else
  java -jar docs-server-vertx-fat.jar &
fi