/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2022-03-26 17:57:07                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-03 13:19:17                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.service;

import com.da.docs.VertxHolder;

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
  private WebClient client = WebClient.create(VertxHolder.vertx == null ? Vertx.vertx() : VertxHolder.vertx,
      new WebClientOptions().setTrustAll(true).setVerifyHost(false));

  public Future<String> get(String url) {
    return request(HttpMethod.GET, url, null);
  }

  public Future<String> post(String url, JSObject data) {
    return request(HttpMethod.POST, url, data);
  }

  public Future<String> put(String url, JSObject data) {
    return request(HttpMethod.PUT, url, data);
  }

  public Future<String> delete(String url, JSObject data) {
    return request(HttpMethod.DELETE, url, data);
  }

  public Future<String> request(HttpMethod method, String url, JSObject data) {
    HttpRequest<Buffer> request = null;
    if (method == HttpMethod.GET) {
      request = client.getAbs(url);
    } else if (method == HttpMethod.POST) {
      request = client.postAbs(url);
    } else if (method == HttpMethod.PUT) {
      request = client.putAbs(url);
    } else if (method == HttpMethod.DELETE) {
      request = client.deleteAbs(url);
    } else {
      request = client.getAbs(url);
    }

    if (data != null) {
      request.putHeader("Content-Type", "application/json");
      request.putHeader("Accept", "application/json");
      request.sendJson(data);
    }
    Future<HttpResponse<Buffer>> response = request.send();

    return response.compose(res -> {
      log.debug("{} {}", method, url);
      log.debug(res.body());
      return Future.succeededFuture(res.bodyAsString());
    }).onFailure(err -> {
      log.error("HTTP {} request to {} failed: {}", method, url, err.getMessage());
    });

  }

  /**
   * need "LastCookie" for any login
   */
  public Future<Buffer> getFile(String url) {
    HttpRequest<Buffer> request = null;
    request = client.getAbs(url);

    Future<HttpResponse<Buffer>> response = request.send();

    return response.compose(res -> {
      return Future.succeededFuture(res.body());
    });
  }

}
