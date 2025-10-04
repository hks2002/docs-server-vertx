/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-04 16:11:46                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.DMSServices;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorizations;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@GetMapping("/docs-api/searchDocsFromTLSNEW")
public class SearchDocsFromTLSNEWHandler implements Handler<RoutingContext> {
  public SearchDocsFromTLSNEWHandler() {
  }

  @Override
  public void handle(RoutingContext context) {
    User user = Optional.ofNullable(context.user()).orElse(User.create(new JsonObject()));

    Authorizations authorizations = user.authorizations();
    if (!authorizations.verify(PermissionBasedAuthorization.create("DOCS_READ"))) {
      log.trace("{},{}", user.principal(), authorizations);
      Response.forbidden(context);
      return;
    }

    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    String PN = request.getParam("PN").toUpperCase();

    DMSServices.getDocuments(PN)
        .onSuccess(docs -> {
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
          if (docs.size() > 0) {
            response.end(docs.encode());
          } else {
            response.end("[]");
          }
        }).onFailure(err -> {
          log.error("Search docs from TLSNEW failed: {}, {}", PN, err.getMessage());
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
          response.end("[]");
        });
  }
}
