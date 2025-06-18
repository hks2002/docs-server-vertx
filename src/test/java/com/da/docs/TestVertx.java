/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-05-19 16:13:27                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-05 11:11:31                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestVertx {

  @Test
  void testFilePath(Vertx vertx, VertxTestContext testContext) throws Throwable {
    vertx.fileSystem().exists("Y://9/9C1/001/9C1001_H.pdf")
        .onSuccess(ar -> {
          log.info("Y:/9/9C1/001/9C1001_H.pdf exists: {}", ar);
          testContext.completeNow();
        })
        .onFailure(err -> {
          log.error("Y:/9/9C1/001/9C1001_H.pdf not exists, please check your path!", err);
          testContext.failNow("Y:/ not exists, please check your path!");
        });

  }

}
