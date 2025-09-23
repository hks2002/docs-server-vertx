/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-18 20:13:24                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.UserAccessLogService;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
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

    String limit = request.getParam("limit", "100");
    String offset = request.getParam("offset", "0");
    User u = context.user();
    UserAccessLogService userAccessLogService = new UserAccessLogService();

    userAccessLogService
        .searchUserAccessLogByLoginName(
            JsonObject.of("login_name", u.principal().getString("offset"), "limit", limit, "offset", offset))
        .onSuccess(list -> {
          if (list.isEmpty()) {
            Response.notFound(context, "No BP found");
            return;
          } else {
            Response.success(context, list);
          }
        })
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          Response.internalError(context);
        });

  }
}
