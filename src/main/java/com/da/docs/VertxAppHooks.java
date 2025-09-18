/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-05-19 16:54:08                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-18 14:03:32                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


package com.da.docs;

import com.da.docs.config.DocsConfig;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxAppHooks implements VertxApplicationHooks {

  private Vertx vertx;

  @Override
  public JsonObject afterVertxOptionsParsed(JsonObject vertxOptions) {
    log.info("VertxOptions:\n{}", vertxOptions.encodePrettily());
    DocsConfig.vertxOptions = new VertxOptions(vertxOptions);
    return vertxOptions;
  }

  @Override
  public JsonObject afterConfigParsed(JsonObject config) {
    ConfigRetrieverOptions retrieveOptions = new ConfigRetrieverOptions();

    retrieveOptions.addStore(new ConfigStoreOptions().setOptional(true).setType("file")
        .setConfig(JsonObject.of("path", "config.json")));
    retrieveOptions.addStore(new ConfigStoreOptions().setOptional(true).setType("file")
        .setConfig(JsonObject.of("path", "config-prod.json")));
    retrieveOptions.addStore(new ConfigStoreOptions().setOptional(true).setType("file")
        .setConfig(JsonObject.of("path", "config-test.json")));
    ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, retrieveOptions);
    try {
      // Load default config, ❗️❗️❗️ blocking call ❗️❗️❗️
      // this config could passing by command line with args
      // -config=#{absolutePath.Config}
      JsonObject defaultConfig = cfgRetriever.getConfig().toCompletionStage().toCompletableFuture().get();
      defaultConfig.getMap().forEach((k, v) -> {
        config.put(k, v);
      });
    } catch (Exception e) {
      log.error("{}", e);
    }

    DocsConfig.config = config;

    log.info("Final Config: \n{}",
        config.encodePrettily()
            // .replaceAll("(\\\"user\\\" : \\\").*(\\\",)", "$1******$2")
            .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2"));
    return config;
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
    vertx = context.vertx();
  }

  @Override
  public void afterVerticleDeployed(HookContext context) {
    // log.info("Hooray! VertxApplication Started! Running background tasks...");
    // FSUtils.setFolderInfo(context.vertx().fileSystem(), "Z:/", "Y:/", 2, 3,
    // "COPY");
    // FSUtils.cleanDBDoc(context.vertx().fileSystem(), "Y:/");
  }

  @Override
  public void afterFailureToStartVertx(HookContext context, Throwable t) {
    log.error("{}", t.getMessage());
  }

  @Override
  public void afterFailureToDeployVerticle(HookContext context, Throwable t) {
    log.error("{}", t.getMessage());
    context.vertx().close();
  }

}
