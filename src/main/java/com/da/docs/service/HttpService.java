/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2022-03-26 17:57:07                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-18 20:04:12                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.da.docs.config.DocsConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

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

  /**
   * Caffeine cache
   */
  private static Cache<String, String> cookiesCache = Caffeine
      .newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES)
      .maximumSize(10000)
      .build();
  private static WebClientOptions options = new WebClientOptions().setTrustAll(true).setVerifyHost(false);
  private static WebClient client = null;
  private static Vertx vertx = null;

  public static Future<String> get(String url, String auth) {
    return request(HttpMethod.GET, url, null, auth);
  }

  public static Future<String> post(String url, JSObject data, String auth) {
    return request(HttpMethod.POST, url, data, auth);
  }

  public static Future<String> put(String url, JSObject data, String auth) {
    return request(HttpMethod.PUT, url, data, auth);
  }

  public static Future<String> delete(String url, JSObject data, String auth) {
    return request(HttpMethod.DELETE, url, data, auth);
  }

  public static void setVertx(Vertx vertx) {
    HttpService.vertx = vertx;
  }

  public static Future<String> request(HttpMethod method, String url, JSObject data, String auth) {
    if (client == null) {
      if (vertx == null) {
        vertx = Vertx.vertx(DocsConfig.vertxOptions);
      }
      client = WebClient.create(vertx, options);
    }

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

    if (auth != null) {
      request.putHeader("authorization", auth);
    }
    // Cookie
    if (auth != null && cookiesCache.getIfPresent(auth) != null) {
      request.putHeader("Cookie", cookiesCache.getIfPresent(auth));
    }
    if (data != null) {
      request.putHeader("Content-Type", "application/json");
      request.putHeader("Accept", "application/json");
      request.sendJson(data);
    }
    Future<HttpResponse<Buffer>> response = request.send();

    return response.compose(res -> {
      // Cookie
      List<String> cookies = res.cookies();
      List<String> cookieCache = new ArrayList<String>();
      for (String cookie : cookies) {
        cookieCache.add(cookie.split(";")[0]);
      }
      if (auth != null) {
        String cookieStr = String.join(";", cookieCache);
        cookiesCache.put(auth, cookieStr);
        log.debug("cookie:{}", cookieStr);

        // save last cookie, for request need login
        cookiesCache.put("LastCookie", cookieStr);
      }

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
  public static Future<Buffer> getFile(String url, String auth) {
    if (client == null) {
      if (vertx == null) {
        vertx = Vertx.vertx();
      }
      client = WebClient.create(vertx, options);
    }

    HttpRequest<Buffer> request = null;
    request = client.getAbs(url);
    if (auth != null) {
      request.putHeader("authorization", auth);
    }
    // Cookie
    if (auth != null && cookiesCache.getIfPresent(auth) != null) {
      request.putHeader("Cookie", cookiesCache.getIfPresent(auth));
    }
    Future<HttpResponse<Buffer>> response = request.send();

    return response.compose(res -> {
      // Cookie
      List<String> cookies = res.cookies();
      List<String> cookieCache = new ArrayList<String>();
      for (String cookie : cookies) {
        cookieCache.add(cookie.split(";")[0]);
      }
      if (auth != null) {
        String cookieStr = String.join(";", cookieCache);
        cookiesCache.put(auth, cookieStr);
        log.debug("cookie:{}", cookieStr);

        // save last cookie, for request need login
        cookiesCache.put("LastCookie", cookieStr);
      }

      return Future.succeededFuture(res.body());
    });
  }

}
