/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2026-01-15 18:53:58                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2026-01-15 19:04:53                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.http.ServerWebSocket;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class MessageService {

  // ws -> username
  private static final Map<ServerWebSocket, String> wsUserMap = new ConcurrentHashMap<>();

  // username -> ws
  private static final Map<String, ServerWebSocket> userWsMap = new ConcurrentHashMap<>();

  /**
   * Adds a user to the collection of connected users
   * 
   * @param ws       The WebSocket connection of the client
   * @param username The username of the client
   */
  public static void addUser(ServerWebSocket ws, String username) {
    if (wsUserMap.containsKey(ws))
      return;

    wsUserMap.put(ws, username);
    ServerWebSocket old = userWsMap.put(username, ws);
    if (old != null && old != ws) {
      removeUser(old);
    }
  }

  /**
   * Removes a user from the collection of connected users, the websocket
   * connection will also be closed
   * 
   * @param ws The identifier of the client to be removed
   */
  public static void removeUser(ServerWebSocket ws) {
    String username = wsUserMap.remove(ws);
    if (username != null) {
      userWsMap.remove(username);
    }
  }

  /**
   * Send a message to a specific client
   * 
   * @param clientId The identifier of the client to send the message to
   * @param message  The message to be sent
   */
  public static void sendToUser(String username, String message) {
    if ("all".equalsIgnoreCase(username)) {
      broadcast(message);
      return;
    }

    ServerWebSocket ws = userWsMap.get(username);
    send(ws, message);
  }

  public static void broadcast(String message) {
    wsUserMap.keySet().forEach(ws -> send(ws, message));
  }

  private static void send(ServerWebSocket ws, String message) {
    if (ws == null || ws.isClosed())
      return;

    if (ws.writeQueueFull()) {
      // 客户端太慢，直接丢或记录
      return;
    }

    ws.writeTextMessage(message);
  }
}
