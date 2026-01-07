/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2022-03-26 17:57:07                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 17:05:51                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import com.da.docs.VertxApp;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.log4j.Log4j2;
import netscape.javascript.JSObject;

@Log4j2
public class HttpService {
  private static WebClient client = WebClient.create(VertxApp.vertx == null ? Vertx.vertx() : VertxApp.vertx,
      new WebClientOptions().setTrustAll(true).setVerifyHost(false));

  public static Future<String> get(String url) {
    return request(HttpMethod.GET, url, null);
  }

  public static Future<String> post(String url, JSObject data) {
    return request(HttpMethod.POST, url, data);
  }

  public static Future<String> put(String url, JSObject data) {
    return request(HttpMethod.PUT, url, data);
  }

  public static Future<String> delete(String url, JSObject data) {
    return request(HttpMethod.DELETE, url, data);
  }

  public static Future<String> request(HttpMethod method, String url, JSObject data) {
    HttpRequest<Buffer> request = null;

    switch (method.name()) {
      case "GET" -> request = client.getAbs(url);
      case "POST" -> request = client.postAbs(url);
      case "PUT" -> request = client.putAbs(url);
      case "DELETE" -> request = client.deleteAbs(url);
      default -> request = client.getAbs(url);
    }

    Future<HttpResponse<Buffer>> response = null;
    if (data != null && (method == HttpMethod.POST || method == HttpMethod.PUT)) {
      request.putHeader("Content-Type", "application/json");
      request.putHeader("Accept", "application/json");
      response = request.sendJson(data);
    } else {
      response = request.send();
    }

    return response.compose(res -> {
      log.debug("{} {}", method, url);
      log.debug("\n{}", res.body());
      return Future.succeededFuture(res.bodyAsString());
    }).onFailure(err -> {
      log.error("HTTP {} request to {} failed: {}", method, url, err.getMessage());
    });

  }

  /**
   * need "LastCookie" for any login
   */
  public static Future<Buffer> getFile(String url) {
    HttpRequest<Buffer> request = null;
    request = client.getAbs(url);

    Future<HttpResponse<Buffer>> response = request.send();

    return response.compose(res -> {
      return Future.succeededFuture(res.body());
    });
  }

}
