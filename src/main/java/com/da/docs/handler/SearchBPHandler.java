/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-19 00:10:31                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


package com.da.docs.handler;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.BPService;
import com.da.docs.utils.Response;

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
public class SearchBPHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();

    String BPCode = request.getParam("BP_CODE");
    BPService bpService = new BPService();

    bpService.searchBPByCode(JsonObject.of("BPCode", BPCode))
        .onSuccess(list -> {
          if (list.isEmpty()) {
            Response.notFound(context, "No BP found");
            return;
          } else {
            context.session().put("BP_CODE", list.get(0).getString("BPCode"));
            context.session().put("BP_NAME", list.get(0).getString("BPName"));
            Response.success(context, list.get(0));
          }
        })
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          Response.internalError(context);
        });

  }
}
