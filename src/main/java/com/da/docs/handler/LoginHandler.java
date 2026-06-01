/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-25 12:41:43                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.PostMapping;
import com.da.docs.service.UserService;
import com.da.docs.serviceStatic.RESPONSE;
import com.da.docs.utils.CommonUtils;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.UserContextImpl;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PostMapping("/docs-api/login")
public class LoginHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
    UsernamePasswordCredentials credentials = CommonUtils.getCredentials(authorization);
    if (credentials == null) {
      RESPONSE.unauthorized(context, "Missing Authorization header");
      return;
    }

    UserService userService = new UserService(context.vertx());
    String ip = CommonUtils.getTrueRemoteIp(request);

    userService.login(credentials, ip)
        .onFailure(err -> {
          RESPONSE.unauthorized(context, err.getMessage());
        })
        .onSuccess(user -> {
          ((UserContextImpl) context.userContext()).setUser(user);
          RESPONSE.success(context, user.principal());
        });
  }

}
