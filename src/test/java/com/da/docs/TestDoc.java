/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-24 16:21:49                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.service.DocsService;
import com.da.docs.serviceStatic.DB;
import com.da.docs.serviceStatic.FS;
import com.da.docs.utils.ConfigUtils;

import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
@Timeout(value = 600, timeUnit = TimeUnit.SECONDS)
public class TestDoc {

  @Test
  void testCleanNonExistsDocs(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-prod.json").onSuccess((config) -> {
      DB.setup(vertx);
      FS.setup(vertx);

      DocsService docsService = new DocsService();
      docsService.cleanNonExistsDocs("NoUser")
          .onSuccess((ar) -> {
            log.info("cleanDBNonExistsDocs success");
            testContext.completeNow();
          })
          .onFailure(e -> {
            log.error("cleanDBNonExistsDocs failed: {}", e.getCause());
            testContext.failNow(e.getMessage());
          });
    });
  }

  @Test
  public void testCleanDuplicatedDocs(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      DB.setup(vertx);
      FS.setup(vertx);

      DocsService docsService = new DocsService();

      docsService.cleanDuplicatedDocs("NoUser")
          .onSuccess((ar1) -> {
            log.info("cleanDuplicatedDocs success");
            testContext.completeNow();
          })
          .onFailure(e -> {
            log.error("cleanDBNonExistsDocs failed: {}", e.getCause());
            testContext.failNow(e.getMessage());
          });
    });
  }
}