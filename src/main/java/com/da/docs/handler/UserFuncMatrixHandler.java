/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:36:11                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.GetMapping;
import com.da.docs.annotation.Permission;
import com.da.docs.service.UserFuncService;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@GetMapping("/docs-api/userFuncMatrix")
@Permission("ADMIN")
public class UserFuncMatrixHandler implements Handler<RoutingContext> {
  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    Integer limit = Integer.parseInt(request.getParam("limit", "100"));
    Integer offset = Integer.parseInt(request.getParam("offset", "0"));
    String loginName = context.user().principal().getString("login_name");
    UserFuncService userFuncService = new UserFuncService(context.vertx());

    userFuncService
        .searchUserFuncMatrix(
            JsonObject.of("login_name", loginName, "limit", limit, "offset", offset))
        .onSuccess(list -> {
          context.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").end(list.toString());
        })
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
          RESPONSE.internalError(context);
        });

  }
}