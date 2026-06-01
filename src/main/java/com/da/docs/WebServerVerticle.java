/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-08 19:11:51                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-21 23:32:41                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import com.da.docs.handler.WebSocketHandler;
import com.da.docs.serviceStatic.DB;
import com.da.docs.serviceStatic.DMS;
import com.da.docs.serviceStatic.FS;
import com.da.docs.serviceStatic.HTTP;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebServerVerticle extends VerticleBase {
  @Override
  public Future<?> start() {
    JsonObject httpConfig = config().getJsonObject("http");
    DB.setup(vertx);
    DMS.setup(vertx);
    FS.setup(vertx);
    HTTP.setup(vertx);
    RESPONSE.setup(vertx);

    return vertx.createHttpServer(new HttpServerOptions(httpConfig))
        .requestHandler(new WebRouter(vertx))
        .webSocketHandler(new WebSocketHandler())
        .listen()
        .onSuccess(http -> {
          log.info("HTTP server started on port {}", http.actualPort());
        });
  }

  @Override
  public Future<?> stop() {
    return Future.succeededFuture();
  }
}
