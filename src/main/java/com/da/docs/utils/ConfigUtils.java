/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2026-01-04 17:50:32                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:01:26                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.utils;

import com.da.docs.VertxApp;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.HookContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConfigUtils {

  public static Future<JsonObject> setUpConfig(Vertx vertx, String configPath) {
    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
        .addStore(new ConfigStoreOptions()
            .setType("file")
            .setFormat("json")
            .setConfig(JsonObject.of("path", configPath, "encoding", "UTF-8")));

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    return retriever.getConfig()
        .onSuccess(cfg -> {
          // ✅ Merge config into vertx context,
          // so vertx.getOrCreateContext().config() can use it
          vertx.getOrCreateContext().config().mergeIn(cfg);
        });
  }

  public static void logConfig(JsonObject config) {
    String safeStr = config
        .encodePrettily()
        .replaceAll("(\\\"password\\\" : \\\").*(\\\",)", "$1******$2");
    log.info("Config:\n{}", safeStr);
  }

  public static void retrieveConfig(HookContext context) {
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
      retrieveOptions.setScanPeriod(3000);
      retrieveOptions.addStore(new ConfigStoreOptions()
          .setOptional(true)
          .setType("file")
          .setFormat("json")
          .setConfig(JsonObject.of("path", configArg, "encoding", "UTF-8")));

      Vertx vertx = context.vertx();
      Context ctx = vertx.getOrCreateContext();
      ConfigRetriever cfgRetriever = ConfigRetriever.create(vertx, retrieveOptions);

      cfgRetriever.listen(change -> {
        // Ignore if the config has not changed, the first time the config is loaded
        JsonObject oldConfig = change.getPreviousConfiguration();
        JsonObject newConfig = change.getNewConfiguration();

        if (oldConfig.isEmpty()) {
          return;
        }

        logConfig(newConfig);
        ctx.config().mergeIn(newConfig);
      });
    }
  }
}
