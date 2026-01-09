/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-18 19:04:16                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.DocsService;
import com.da.docs.utils.FSUtils;
import com.da.docs.utils.Response;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
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
@GetMapping("/docs-api/searchDocs")
public class SearchDocsHandler implements Handler<RoutingContext> {
  public SearchDocsHandler() {
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

    String PN = request.getParam("PN");
    DocsService docsService = new DocsService();

    docsService.searchDocsByName(JsonObject.of("file_name", "%" + PN + "%"))
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end("[]");
        })
        .compose(list -> {
          List<Future<JsonObject>> futures = new ArrayList<>();

          for (JsonObject f : list) {
            String fileName = f.getString("file_name");

            Future<JsonObject> futureDoc = Future.all(FSUtils.getDocsRoot(), FSUtils.getFolderPathByFileName(fileName))
                .compose(data -> {
                  String docRoot = data.resultAt(0);
                  String toFolderPath = data.resultAt(1);
                  String toFileFullPath = docRoot + '/' + toFolderPath + '/' + fileName;
                  log.debug("toFileFullPath: {}", toFileFullPath);

                  return FSUtils.isFileExists(toFileFullPath).compose(fileExists -> {
                    if (fileExists) {
                      return FSUtils.fs.props(toFileFullPath).compose(props -> {
                        // make sure the info is correct based on the file,
                        // not the database,database may be wrong
                        JsonObject o = new JsonObject();
                        o.put("name", fileName);
                        o.put("size", props.size());
                        o.put("lastModified", props.lastModifiedTime());
                        o.put("url", "/docs-api/docs/" + toFolderPath + '/' + fileName);

                        // for updating the doc info
                        f.put("size", props.size());
                        f.put("lastModified", props.lastModifiedTime());
                        docsService.modifyDocs(f);

                        return Future.succeededFuture(o);
                      });
                    }
                    return Future.failedFuture("File not found" + toFileFullPath);

                  });

                });
            futures.add(futureDoc);
          } // end of for list

          return Future.join(futures).onComplete(ar -> {
            if (ar.succeeded()) {
              CompositeFuture cf = ar.result();

              // 遍历每个 future 的结果
              List<JsonObject> resultList = new ArrayList<>();
              for (int i = 0; i < cf.size(); i++) {
                if (cf.succeeded(i)) {
                  resultList.add(cf.resultAt(i)); // ✅ resultAt(i)
                } else {
                  log.warn("Future at index {} failed: {}", i, cf.cause(i).getMessage());
                }
              }

              log.debug("resultList:{}", resultList);
              response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end(resultList.toString());
            } else {
              log.error("Join failed: {}", ar.cause());
              response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end("[]");
            }
          });
        });

  }
}
