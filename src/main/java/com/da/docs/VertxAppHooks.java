/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-05-19 16:54:08                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 17:01:28                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxAppHooks implements VertxApplicationHooks {

  @Override
  public JsonObject afterVertxOptionsParsed(JsonObject vertxOptions) {
    log.debug("VertxOptions:\n{}", vertxOptions.encodePrettily());
    return vertxOptions;
  }

  @Override
  public JsonObject afterConfigParsed(JsonObject config) {
    String safeStr = config
        .encodePrettily()
        .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2");
    log.info("Config:\n{}", safeStr);
    VertxApp.appConfig = config;
    return config;
  }

  @Override
  public VertxBuilder createVertxBuilder(VertxOptions options) {
    return Vertx.builder().with(options);
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
    VertxApp.vertx = context.vertx();
    VertxApp.fs = VertxApp.vertx.fileSystem();
  }

  @Override
  public void afterVerticleDeployed(HookContext context) {
    boolean hasConfigArg = false;
    String configArg = null;
    for (String s : VertxApp.appArgs) {
      if (s.startsWith("-conf=") || s.startsWith("--conf=")) {
        hasConfigArg = true;
        configArg = s.replaceFirst("-{1,2}conf=", "");
        break;
      }
    }

    if (hasConfigArg && configArg != null) {
      ConfigRetrieverOptions retrieveOptions = new ConfigRetrieverOptions();

      retrieveOptions.addStore(new ConfigStoreOptions()
          .setOptional(true)
          .setType("file").setFormat("json")
          .setConfig(JsonObject.of("path", configArg)));

      retrieveOptions.setScanPeriod(3000);
      ConfigRetriever cfgRetriever = ConfigRetriever.create(context.vertx(), retrieveOptions);

      cfgRetriever.listen(change -> {
        // Ignore if the config has not changed, the first time the config is loaded
        if (VertxApp.appConfig.equals(change.getNewConfiguration())) {
          return;
        }

        VertxApp.appConfig = change.getNewConfiguration();
        String safeStr = VertxApp.appConfig
            .encodePrettily()
            .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2");
        log.info("New Config: \n{}", safeStr);
      });
    }
  }

  @Override
  public void afterVertxStarted(HookContext context) {
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
