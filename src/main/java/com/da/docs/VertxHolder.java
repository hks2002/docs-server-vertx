/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-09-18 12:08:15                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-12-25 16:07:25                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxHolder {
  static public VertxOptions vertxOptions = null;
  static public JsonObject appConfig = null;
  static public Vertx vertx = null;
  static public FileSystem fs = null;

  public static void init(Vertx vertx) {
    VertxHolder.vertx = vertx;
    VertxHolder.fs = vertx.fileSystem();

    ConfigRetrieverOptions retrieveOptions = new ConfigRetrieverOptions();

    retrieveOptions.addStore(new ConfigStoreOptions()
        .setOptional(true)
        .setType("file").setFormat("json")
        .setConfig(JsonObject.of("path", "config.json")));
    retrieveOptions.addStore(new ConfigStoreOptions()
        .setOptional(true)
        .setType("file")
        .setFormat("json")
        .setConfig(JsonObject.of("path", "config-prod.json")));
    retrieveOptions.addStore(new ConfigStoreOptions()
        .setOptional(true)
        .setType("file")
        .setFormat("json")
        .setConfig(JsonObject.of("path", "config-test.json")));
    retrieveOptions.addStore(new ConfigStoreOptions()
        .setOptional(true)
        .setType("file")
        .setFormat("json")
        .setConfig(JsonObject.of("path", "config-dev.json")));

    // retrieveOptions.setScanPeriod(3000);
    ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, retrieveOptions);

    cfgRetriever.listen(change -> {
      log.info("{}", change.getNewConfiguration().encodePrettily());
    });

    try {
      VertxHolder.appConfig = cfgRetriever.getConfig().toCompletionStage().toCompletableFuture().get();
      log.info("Final Config: \n{}",
          appConfig.encodePrettily()
              // .replaceAll("(\\\"user\\\" : \\\").*(\\\",)", "$1******$2")
              .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2"));
    } catch (Exception e) {
      log.error("{}", e);
    }
  }
}
