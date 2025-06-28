/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-28 21:08:48                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.AllMapping;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
//import io.vertx.core.http.HttpHeaders;
//import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
//import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.impl.UserContextImpl;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllMapping
public class SessionHandler implements Handler<RoutingContext> {
  private static String adServer = "ldap://127.0.0.1:389";
  private static String adDomain = "docs.com";
  private static String adSearchBase = "DC=docs,DC=com";

  public SessionHandler(JsonObject config) {
    JsonObject adConfig = Optional.ofNullable(config.getJsonObject("adServer")).orElse(new JsonObject());
    adServer = adConfig.getString("url", adServer);
    adDomain = adConfig.getString("domain", adDomain);
    adSearchBase = adConfig.getString("searchBase", adSearchBase);
  }

  @Override
  public void handle(RoutingContext context) {
    String requestPath = context.normalizedPath();
    User u = context.user();

    if (requestPath.equals("/docs-api/server-info") ||
        requestPath.equals("/docs-api/login") ||
        requestPath.equals("/docs-api/logout")) {
      context.next();
    } else if (requestPath.equals("/docs-api/check-session")) {
      if (u == null) {
        log.debug("user not cached in session");
        Response.unauthorized(context, "Session expired or not logged in");
      } else {
        log.debug("user: {} cached in session", u.principal());
        Response.success(context, "Session is active");
      }
      return;
    } else {
      if (u == null) {
        Response.unauthorized(context, "Session expired or not logged in");
        return;
      } else {// session cached user, no need to check again
        log.debug("user: {} cached in session", u.principal());
        context.next();
      }

    }

  }

}
