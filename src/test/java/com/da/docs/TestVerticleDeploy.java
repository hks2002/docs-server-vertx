/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-05-19 16:13:27                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-03 10:11:42                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TestVerticleDeploy {

  @Test
  void verticleDeploy(Vertx vertx, VertxTestContext testContext) throws Throwable {
    VertxApp.main(new String[] { "com.da.docs.WebServerVerticle" });
    testContext.completeNow();
  }
}
