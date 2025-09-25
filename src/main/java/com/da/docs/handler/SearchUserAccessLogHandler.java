/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-25 13:40:05                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs.handler;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.UserAccessLogService;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@GetMapping("/docs-api/userAccessLog")
public class SearchUserAccessLogHandler implements Handler<RoutingContext> {

  public SearchUserAccessLogHandler() {
  }

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    Integer limit = Integer.parseInt(request.getParam("limit", "100"));
    Integer offset = Integer.parseInt(request.getParam("offset", "0"));
    User u = context.user();
    UserAccessLogService userAccessLogService = new UserAccessLogService();

    userAccessLogService
        .searchUserAccessLogByLoginName(
            JsonObject.of("login_name", u.principal().getString("login_name"), "limit", limit, "offset", offset))
        .onSuccess(list -> {
          JsonArray json = new JsonArray();
          for (JsonObject o : list) {
            o.put("location", "docs-api/docs/" + o.getString("location"));
            json.add(o);
          }
          context.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").end(json.encode());
        })
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          Response.internalError(context);
        });

  }
}
