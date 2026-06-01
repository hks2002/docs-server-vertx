/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-22 09:34:10                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestConfig {

  @Test
  public void testDB(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
        .addStore(new ConfigStoreOptions()
            .setType("file")
            .setConfig(new JsonObject()
                .put("path", "config.json")));

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
    retriever.getConfig().onSuccess(config -> {
      log.info("Config: {}", config);

      vertx.getOrCreateContext().config().mergeIn(config);
      log.info("Config: {}", vertx.getOrCreateContext().config());
      testContext.completeNow();
    });
  }

}