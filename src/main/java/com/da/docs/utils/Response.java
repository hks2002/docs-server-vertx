/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-07-10 21:15:02                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.utils;

import java.util.List;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Response {

  public static void internalError(RoutingContext ctx) {
    internalError(ctx, "Server error");
  }

  public static void internalError(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);
    log.error(msg);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(500);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/500");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(500);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void unauthorized(RoutingContext ctx) {
    unauthorized(ctx, "Unauthorized");
  }

  public static void unauthorized(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(401);
        response.putHeader("WWW-Authenticate", "Basic realm=\"Ad Authorization\"");
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/login");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(401);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void forbidden(RoutingContext ctx) {
    forbidden(ctx, "Forbidden");
  }

  public static void forbidden(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(403);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/403");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(403);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void badRequest(RoutingContext ctx) {
    badRequest(ctx, "Bad Request");
  }

  public static void badRequest(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    log.warn(msg);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(400);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/400");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(400);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void notFound(RoutingContext ctx) {
    notFound(ctx, "Not Found");
  }

  public static void notFound(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    log.warn(msg);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(404);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/404");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(404);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void success(RoutingContext ctx) {
    success(ctx, "Success");
  }

  public static void success(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void success(RoutingContext ctx, JsonObject msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg).encode());
        break;
      default:
        response.end(msg.encode());
    }
  }

  public static void success(RoutingContext ctx, JsonArray msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg).encode());
        break;
      default:
        response.end(msg.encode());
    }
  }

  public static void success(RoutingContext ctx, List<JsonObject> msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg.toArray()).encode());
        break;
      default:
        response.end(msg.toArray().toString());
    }
  }
}
