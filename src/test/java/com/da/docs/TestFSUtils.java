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

import com.da.docs.utils.FSUtils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestFSUtils {

  @Test
  public void testLog(Vertx vertx, VertxTestContext testContext) throws Throwable {
    FSUtils.setup("c:/docs", 2, 3, vertx);

    FSUtils.fs.readFile("sqlTemplate/insertLog.sql")
        .onSuccess(ar -> {
          log.info("success: {}", ar.toString());
        }).onFailure(e -> {
          log.error("Error:{}", e.getMessage());
        }).compose(ar -> {
          log.info("compose: {}", ar.toString());
          return Future.succeededFuture();
        }).andThen((a) -> {
          testContext.completeNow();
        });

  }

}
