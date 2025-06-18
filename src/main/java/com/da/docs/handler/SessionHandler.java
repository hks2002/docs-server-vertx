/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-05-24 01:38:26                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.RouteMapping;
import com.da.docs.service.UserService;
import com.da.docs.utils.CommonUtils;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.UserContextImpl;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RouteMapping
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
    if (context.normalizedPath().equals("/docs-api/server-info")) {
      context.next();

    } else {
      User u = context.user();

      if (u == null) {
        HttpServerRequest request = context.request();
        UsernamePasswordCredentials credentials = CommonUtils
            .getCredentials(request.headers().get(HttpHeaders.AUTHORIZATION));
        if (credentials == null) {
          Response.unauthorized(context, "Missing Authorization header");
          return;
        }

        UserService userService = new UserService();
        String ip = CommonUtils.getTrueRemoteIp(request);

        userService.login(adServer, adDomain, adSearchBase, credentials, ip)
            .onFailure(err -> {
              Response.unauthorized(context, err.getMessage());
            })
            .onSuccess(user -> {
              ((UserContextImpl) context.userContext()).setUser(user);
              context.next();
            });

      } else {// session cached user, no need to check again
        log.debug("user: {} cached in session", u.principal());
        context.next();
      }

    }

  }

}
