/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-10 20:02:13                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.serviceStatic.FS;
import com.da.docs.utils.ConfigUtils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestFS {

  @Test
  public void testFS(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      FS.setup(vertx);

      FS.fs.readFile("sqlTemplate/insertLog.sql")
          .onSuccess(ar -> {
            log.info("success: {}", ar.toString());
            testContext.completeNow();
          }).onFailure(e -> {
            log.error("Error:{}", e.getCause());
            testContext.failNow(e.getMessage());
          });
    });

  }

  @Test
  public void testHardLink(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      FS.setup(vertx);

      FS.fs.link("c:/docs/insertLog.link.sql", "c:/docs/insertLog.sql")
          .onSuccess(ar -> {
            log.info("success: hard link created");
            testContext.completeNow();
          }).onFailure(e -> {
            log.error("Error:{}", e.getCause());
            testContext.failNow(e.getMessage());
          });
    });
  }

  @Test
  public void testHardLink1(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      FS.setup(vertx);

      FS.fs.lprops("c:/docs/insertLog.link.sql")
          .onSuccess(ar -> {
            log.info("success: {}", ar.isSymbolicLink());
            testContext.completeNow();
          }).onFailure(e -> {
            log.error("Error:{}", e.getCause());
            testContext.failNow(e.getMessage());
          });
    });
  }

  @Test
  public void testHardLink2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      FS.setup(vertx);
      Future<Boolean> f1 = FS.fs.exists("Y:/Drawing/C/CP2/529/CP25293_-.pdf");
      Future<Boolean> f2 = FS.fs.exists("Y:/Drawing/C/CP2/529/CP25293_A.pdf");

      Future.all(f1, f2)
          .onSuccess(exists -> {
            log.info("success: {}", exists);
            testContext.completeNow();
          }).onFailure(e -> {
            log.error("Error:{}", e.getCause());
            testContext.failNow(e.getMessage());
          });
    });
  }
}
