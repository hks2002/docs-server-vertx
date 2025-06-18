/**********************************************************************************************************************
 * @Author                : <>                                                                                        *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : <>                                                                                        *
 * @LastEditDate          : 2025-05-20 13:42:06                                                                       *
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
  void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {

    var rst = DMSServices.getDocuments("856A1001");
    log.info("getDocuments: {}", rst);

    vertx.setTimer(2000, id -> {
      testContext.completeNow();
    });

  }

}
