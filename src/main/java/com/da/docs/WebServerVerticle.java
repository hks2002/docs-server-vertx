/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-08 19:11:51                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-02 13:24:01                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebServerVerticle extends VerticleBase {
  @Override
  public Future<?> start() {
    JsonObject config = config().getJsonObject("http");

    // init DB
    DB.initDB(vertx);

    return vertx.createHttpServer(new HttpServerOptions(config))
        .requestHandler(new WebRouter(vertx))
        .listen().onSuccess(http -> {
          log.info("HTTP server started on port {}", http.actualPort());
        });
  }

  @Override
  public Future<?> stop() {
    DB.closeAll();
    return Future.succeededFuture();
  }
}
