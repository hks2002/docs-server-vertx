/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2026-01-15 19:07:35                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.service.MessageService;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebSocketHandler implements Handler<ServerWebSocket> {

  @Override
  public void handle(ServerWebSocket ws) {

    if (!"/docs-ws".equals(ws.path())) {
      ws.close((short) 1008, "Forbidden");
      return;
    }

    ws.textMessageHandler(msg -> {
      try {
        JsonObject json = new JsonObject(msg);

        if (json.containsKey("userName")) {
          String userName = json.getString("userName");
          String userInfo = json.getString("userInfo", "Missing userInfo");

          MessageService.addUser(ws, userName);

          ws.writeTextMessage(
              JsonObject.of("msg", "Hello " + userInfo + "!").encode());
        }
      } catch (Exception e) {
        MessageService.removeUser(ws);
        ws.close();
        log.warn("Invalid WS message from {}: {}", ws.remoteAddress(), msg);
      }
    });

    // back pressure
    ws.setWriteQueueMaxSize(64 * 1024);

    ws.closeHandler(v -> {
      MessageService.removeUser(ws);
      log.debug("WebSocket closed: {}", ws.remoteAddress());
    });

    ws.exceptionHandler(err -> {
      if (ws.isClosed()) {
        log.debug("WebSocket already closed: {}", ws.remoteAddress());
      } else {
        log.warn("WebSocket exception", err);
      }
    });

  }
}