/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-24 16:23:15                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.PostMapping;
import com.da.docs.service.LogService;
import com.da.docs.serviceStatic.RESPONSE;
import com.da.docs.utils.CommonUtils;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PostMapping("/docs-api/logout")
public class LogoutHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    User u = context.user();

    if (u == null) {
      RESPONSE.success(context, "Logout Success");
    } else {

      String accept = CommonUtils.getAccept(context);
      HttpServerRequest request = context.request();
      String ip = CommonUtils.getTrueRemoteIp(request);
      String userName = u.principal().getString("login_name");
      LogService.addLog("LOGOUT_SUCCESS", ip, userName);

      switch (accept) {
        case "application/json":
          context.userContext().clear();
          RESPONSE.success(context, "Logout Success");
          break;
        default:
          context.userContext().logout("/docs-web/#/login")
              .onSuccess((ar) -> {
              })
              .onFailure((ar) -> {
                RESPONSE.internalError(context, "Logout Failed");
              });
      }

    }

  }

}
