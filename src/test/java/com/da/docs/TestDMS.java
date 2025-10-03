/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-03 16:53:27                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

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
    log.info("Test DMSServices");
    DMSServices dmsServices = new DMSServices();
    dmsServices.setDmsServer("http://192.168.0.247:4040");

    dmsServices.getDocuments("956A1001G01").onSuccess(result -> {
      log.info("getDocuments: {}", result);
    }).onFailure(e -> {
      log.error("Error: ", e);
    }).andThen(r -> {
      testContext.completeNow();
    });

  }

}
