package com.da.docs.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.http.ServerWebSocket;

public class MessageService {

  // key is client's address
  private static final Map<String, ServerWebSocket> addressWebSocketMap = new ConcurrentHashMap<>();
  // key is user name, value is client's address
  private static final Map<String, String> userAddressMap = new ConcurrentHashMap<>();

  /**
   * Adds a user to the collection of connected users
   * 
   * @param ws       The WebSocket connection of the client
   * @param username The username of the client
   */
  public static void addUser(ServerWebSocket ws, String username) {
    // Store the WebSocket connection in the map with client's address as the key
    addressWebSocketMap.put(ws.remoteAddress().toString(), ws);
    userAddressMap.put(username, ws.remoteAddress().toString());
  }

  /**
   * Removes a user from the collection of connected users, the websocket
   * connection will also be closed
   * 
   * @param clientId The identifier of the client to be removed
   */
  public static void removeUser(String clientId) {
    addressWebSocketMap.remove(clientId);

    for (Map.Entry<String, String> entry : userAddressMap.entrySet()) {
      if (entry.getValue().equals(clientId)) {
        String userName = entry.getKey();
        userAddressMap.remove(userName);
        break; // quit if found
      }
    }
  }

  /**
   * Send a message to a specific client
   * 
   * @param clientId The identifier of the client to send the message to
   * @param message  The message to be sent
   */
  public static void sendMessageToClient(String clientId, String message) {
    ServerWebSocket ws = addressWebSocketMap.get(clientId);
    if (ws != null && !ws.isClosed()) {
      ws.writeTextMessage(message);
    }
  }

  /**
   * Send a message to a specific user
   * 
   * @param username The username of the user to send the message to
   * @param message  The message to be sent
   */
  public static void sendMessageToUser(String username, String message) {
    if (username.toLowerCase().equals("all")) {
      broadcast(message);
      return;
    }

    String clientId = userAddressMap.get(username);
    if (clientId != null) {
      sendMessageToClient(clientId, message);
    }
  }

  /**
   * Broadcasts a message to all connected clients
   * 
   * @param message The message to be broadcasted to all clients
   */
  public static void broadcast(String message) {
    for (ServerWebSocket ws : addressWebSocketMap.values()) {
      if (!ws.isClosed()) {
        ws.writeTextMessage(message);
      }
    }
  }
}
