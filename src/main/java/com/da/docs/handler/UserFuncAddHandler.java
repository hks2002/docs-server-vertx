/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:22:24                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.Permission;
import com.da.docs.annotation.PostMapping;
import com.da.docs.service.UserFuncService;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@PostMapping("/docs-api/userFuncAdd")
@Permission("ADMIN")
public class UserFuncAddHandler implements Handler<RoutingContext> {
  @Override
  public void handle(RoutingContext context) {
    UserFuncService userFuncService = new UserFuncService(context.vertx());

    userFuncService
        .addUserFunc(context.body().asJsonObject())
        .onSuccess(num -> {
          RESPONSE.success(context);
        })
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
          RESPONSE.internalError(context);
        });

  }
}