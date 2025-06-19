/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-19 10:11:09                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.DMSServices;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.Utils;
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
  private String docsRoot = Utils.isWindows() ? "c:/docs" : "/mnt/docs";
  private int folderDeep = 0;
  private int folderLen = 3;

  public SearchDocsFromTLSNEWHandler(JsonObject config) {
    var docsConfig = Optional.ofNullable(config.getJsonObject("docs")).orElse(new JsonObject());
    var uploadConfig = Optional.ofNullable(config.getJsonObject("upload")).orElse(new JsonObject());
    this.docsRoot = docsConfig.getString("docsRoot", docsRoot);
    this.folderDeep = uploadConfig.getInteger("folderDeep", folderDeep);
    this.folderLen = uploadConfig.getInteger("folderLen", folderLen);
  }

  @Override
  public void handle(RoutingContext context) {
    Vertx vertx = context.vertx();
    User user = context.user();

    Authorizations authorizations = user.authorizations();
    if (!authorizations.verify(PermissionBasedAuthorization.create("DOCS_READ"))) {
      log.trace("{},{}", user.principal(), authorizations);
      Response.forbidden(context);
      return;
    }

    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    String PN = request.getParam("PN").toUpperCase();
    try {
      vertx.executeBlocking(() -> {
        return DMSServices.getDocuments(PN);
      }).onSuccess(ar -> {
        response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (ar.size() > 0) {

          vertx.executeBlocking(() -> {
            DMSServices.downloadDmsDocs(
                context.vertx(),
                ar,
                docsRoot,
                folderDeep,
                folderLen);
            return "";
          }).onFailure(err -> {
            log.error("{}", err.getMessage());
          });

          response.end(ar.encode());
        } else {
          response.end("[]");
        }
      }).onFailure(err -> {
        log.error("Search docs from TLSNEW failed: {}, {}", PN, err.getMessage());
        response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        response.end("[]");
      });

    } catch (Exception e) {
      log.error("{}", e.getMessage());
      Response.internalError(context);
    }
  }
}
