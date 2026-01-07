/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-23 22:25:09                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

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

    // Receive message
    ws.textMessageHandler(msg -> {
      log.debug(ws.remoteAddress() + ":" + msg);

      JsonObject json = new JsonObject(msg);
      if (json.containsKey("userName")) {
        String userName = json.getString("userName");
        String userInfo = json.getString("userInfo", "Missing userInfo");

        MessageService.addUser(ws, userName);

        ws.writeTextMessage(JsonObject.of("msg", "Hello " + userInfo + "!").encode());
      } else {
        ws.writeTextMessage(JsonObject.of("msg", "Message received: " + msg).encode());
      }
    });

    // Close event
    ws.closeHandler(v -> {
      MessageService.removeUser(ws.remoteAddress().toString());
      log.debug("Closed: {}", ws.remoteAddress());
    });

    // Exception event
    ws.exceptionHandler(err -> {
      log.error("Error in WebSocket: {}", err.getMessage());
      err.printStackTrace();
    });
  }

}
