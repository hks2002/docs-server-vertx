/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-07-13 12:44:24                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.util.Optional;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.DocsService;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
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
@GetMapping("/docs-api/searchDocsFromTLSOLD")
public class SearchDocsFromTLSOLDHandler implements Handler<RoutingContext> {
  public SearchDocsFromTLSOLDHandler(JsonObject config) {
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
    DocsService docsService = new DocsService();

    docsService.searchDocsFromTLSOLDByName(JsonObject.of("PN", "%" + PN.replace("'", "") + "%"))
        .onSuccess(list -> {
          JsonArray json = new JsonArray();

          for (JsonObject s : list) {
            JsonObject o = new JsonObject();
            String pathOri = s.getString("Path").replace("\\", "/");
            int index = pathOri.lastIndexOf("/");
            if (index > 0) {
              o.put("file", pathOri.substring(index + 1));
            } else {
              o.put("file", "ERROR");
            }

            if (pathOri.startsWith("[DOCS_SAGEX3]")) {
              pathOri = "file://192.168.10.47/DOCS_SAGEX3/" + pathOri.substring(14);
            } else {
              // Here keep the path
            }
            o.put("path", pathOri);

            json.add(o);
          }
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(json.encode());
        })
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          Response.internalError(context);
        });

  }
}
