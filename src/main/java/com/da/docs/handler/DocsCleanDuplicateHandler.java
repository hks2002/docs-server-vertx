/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:32:07                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.Permission;
import com.da.docs.annotation.PostMapping;
import com.da.docs.service.DocsService;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@PostMapping("/docs-api/clean/duplicate")
@Permission("ADMIN")
public class DocsCleanDuplicateHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    DocsService docsService = new DocsService();
    String userName = Optional
        .ofNullable(context.user().principal())
        .orElse(new JsonObject())
        .getString("login_name", "none");
    docsService.cleanDuplicatedDocs(userName);
    RESPONSE.success(context);
  }
}