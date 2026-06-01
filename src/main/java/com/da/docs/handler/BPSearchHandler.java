/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:32:10                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.GetMapping;
import com.da.docs.annotation.Permission;
import com.da.docs.service.BPService;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@GetMapping("/docs-api/searchBP")
@Permission("DOCS_READ")
public class BPSearchHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    String BPCode = request.getParam("BP_CODE");
    BPService bpService = new BPService();

    bpService.searchBPByCode(JsonObject.of("BPCode", BPCode))
        .onSuccess(list -> {
          if (list.isEmpty()) {
            RESPONSE.notFound(context, "No BP found");
            return;
          } else {
            context.session().put("BP_CODE", list.get(0).getString("BPCode"));
            context.session().put("BP_NAME", list.get(0).getString("BPName"));
            RESPONSE.success(context, list.get(0));
          }
        })
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
          RESPONSE.internalError(context);
        });

  }
}