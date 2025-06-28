/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-28 12:18:47                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.PostMapping;
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
@PostMapping("/docs-api/login")
public class LoginHandler implements Handler<RoutingContext> {
  private static String adServer = "ldap://127.0.0.1:389";
  private static String adDomain = "docs.com";
  private static String adSearchBase = "DC=docs,DC=com";

  public LoginHandler(JsonObject config) {
    JsonObject adConfig = Optional.ofNullable(config.getJsonObject("adServer")).orElse(new JsonObject());
    adServer = adConfig.getString("url", adServer);
    adDomain = adConfig.getString("domain", adDomain);
    adSearchBase = adConfig.getString("searchBase", adSearchBase);
  }

  @Override
  public void handle(RoutingContext context) {
    User u = context.user();

    if (u == null) {
      HttpServerRequest request = context.request();
      String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
      UsernamePasswordCredentials credentials = CommonUtils.getCredentials(authorization);
      if (credentials == null) {
        Response.unauthorized(context, "Missing Authorization header");
        return;
      }

      UserService userService = new UserService();
      String ip = CommonUtils.getTrueRemoteIp(request);

      userService.login(adServer, adDomain, adSearchBase, credentials, ip)
          .onFailure(err -> {
            Response.unauthorized(context, err.getMessage());
          }).onSuccess(user -> {
            ((UserContextImpl) context.userContext()).setUser(user);
            Response.success(context, user.principal());
          });

    } else {// session cached user, no need to check again
      log.debug("user: {} cached in session", u.principal());
      Response.success(context, u.principal());
    }
  }

}
