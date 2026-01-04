/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 19:57:24                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.service.DMSServices;
import com.da.docs.utils.ConfigUtils;
import com.da.docs.utils.FSUtils;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestDMS {

  @Test
  void test2(Vertx vertx, VertxTestContext testContext) throws Throwable {

    String dmsServer = ConfigUtils.getConfig("config-dev.json").getString("dmsServer");
    String dmsServerFast = ConfigUtils.getConfig("config-dev.json").getString("dmsServerFast");

    FSUtils.setup("c:/docs", 3, 3, vertx.fileSystem());
    DMSServices.setup(dmsServer, dmsServerFast, vertx.fileSystem());

    DMSServices.getDocuments("956A1001G02")
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
