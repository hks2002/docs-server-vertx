/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-12-25 16:34:24                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.service.DMSServices;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestDMS {

  @Test
  void test2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    VertxHolder.init(vertx);
    log.info("Test DMSServices");

    DMSServices.getDocuments("956A1001G01")
        .onSuccess(result -> {
          log.info("getDocuments: {}", result.encodePrettily());
        })
        .onFailure(e -> {
          log.error("Error: ", e);
        })
        .andThen(r -> {
          testContext.completeNow();
        });

  }

}
