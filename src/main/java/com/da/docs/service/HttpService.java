/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2022-03-26 17:57:07                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-03 10:21:13                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.da.docs.utils.SSLContextUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class HttpService {

  /**
   * Caffeine cache
   */
  private static Cache<String, String> cache = Caffeine
      .newBuilder()
      .expireAfterAccess(5, TimeUnit.MINUTES)
      .maximumSize(10000)
      .build();

  private static HttpClient client = null;

  public static HttpResponse<String> request(String url, String method) {
    return request(url, method, null, null);
  }

  public static HttpResponse<String> request(
      String url,
      String method,
      String data) {
    return request(url, method, data, null);
  }

  public static HttpResponse<String> request(
      String url,
      String method,
      String data,
      String auth) {
    try {
      // Disable host name verification Globally
      Properties props = System.getProperties();
      props.setProperty(
          "jdk.internal.httpclient.disableHostnameVerification",
          Boolean.TRUE.toString());

      if (client == null) {
        client = HttpClient.newBuilder().sslContext(SSLContextUtils.getMySSLContext()).build();
      }

      Builder reqBuilder = HttpRequest
          .newBuilder()
          .uri(URI.create(url))
          .setHeader("Content-Type", "application/json")
          .setHeader("Accept", "application/json");

      if (auth != null) {
        reqBuilder.header("authorization", auth);
      }
      // Cookie
      if (auth != null) {
        if (cache.getIfPresent(auth) != null) {
          reqBuilder.header("Cookie", cache.getIfPresent(auth));
        }
      }
      log.debug("data:{}", data);

      switch (method) {
        case "GET":
          reqBuilder.GET();
          break;
        case "POST":
          if (data != null && !data.isBlank()) {
            reqBuilder.POST(BodyPublishers.ofString(data));
          }
          break;
        case "PUT":
          if (data != null && !data.isBlank()) {
            reqBuilder.PUT(BodyPublishers.ofString(data));
          }
          break;
        case "DELETE":
          reqBuilder.DELETE();
          break;
        default:
          reqBuilder.GET();
      }

      HttpRequest request = reqBuilder.build();
      HttpResponse<String> response = null;

      response = client.send(request, BodyHandlers.ofString());

      // Cookie
      List<String> cookieResponse = response.headers().allValues("Set-Cookie");
      List<String> cookieCache = new ArrayList<String>();
      for (String cookie : cookieResponse) {
        cookieCache.add(cookie.split(";")[0]);
      }
      if (auth != null) {
        String cookieStr = String.join(";", cookieCache);
        cache.put(auth, cookieStr);
        log.debug("cookie:{}", cookieStr);

        // save last cookie, for request need login
        cache.put("LastCookie", cookieStr);
      }

      log.debug("{}", response.statusCode());
      log.debug(response.body());

      return response;
    } catch (Exception e) {
      e.getStackTrace();
      log.error(e.getLocalizedMessage());
      return null;
    }
  }

  /**
   * need "LastCookie" for any login
   */
  public static byte[] getFile(String url) {
    try {
      // Disable host name verification Globally
      Properties props = System.getProperties();
      props.setProperty(
          "jdk.internal.httpclient.disableHostnameVerification",
          Boolean.TRUE.toString());

      if (client == null) {
        client = HttpClient.newBuilder().sslContext(SSLContextUtils.getMySSLContext()).build();
      }

      Builder reqBuilder = HttpRequest.newBuilder().uri(URI.create(url));

      // Cookie
      if (cache.getIfPresent("LastCookie") != null) {
        reqBuilder.header("Cookie", cache.getIfPresent("LastCookie"));
      }

      reqBuilder.GET();

      HttpRequest request = reqBuilder.build();
      HttpResponse<byte[]> response = null;

      response = client.send(request, BodyHandlers.ofByteArray());

      return response.body();
    } catch (Exception e) {
      e.getStackTrace();
      log.error(e.getLocalizedMessage());
      return null;
    }
  }

}
