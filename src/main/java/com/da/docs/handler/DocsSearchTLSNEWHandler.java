/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:32:20                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import java.util.List;
import java.util.Optional;

import com.da.docs.annotation.GetMapping;
import com.da.docs.annotation.Permission;
import com.da.docs.serviceStatic.DMS;
import com.da.docs.serviceStatic.MSG;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@GetMapping("/docs-api/searchDocsFromTLSNEW")
@Permission("DOCS_READ")
public class DocsSearchTLSNEWHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    String PN = request.getParam("PN").toUpperCase();
    String userName = Optional
        .ofNullable(context.user().principal())
        .orElse(new JsonObject())
        .getString("login_name", "none");

    DMS.getDocuments(PN)
        .onSuccess(docs -> {
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
          if (docs.size() > 0) {

            @SuppressWarnings("unchecked")
            List<JsonObject> list = docs.getList();
            list.sort((a, b) -> {
              return b.getLong("lastModified")
                  .compareTo(a.getLong("lastModified"));
            });
            response.end(list.toString());
          } else {
            response.end("[]");
          }
        })
        .onFailure(err -> {
          log.error("Search docs from TLSNEW failed: {}, {}", PN, err.getCause());
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
          response.end("[]");
        })
        .andThen(docs -> {
          for (int i = 0; i < docs.result().size(); i++) {
            final int index = i;
            // ❗❗❗ to limit the number of documents to be downloaded
            if (index >= 5) {
              break;
            }

            // do actions in background, not block the main thread
            context.vertx().executeBlocking(() -> {

              JsonObject doc = docs.result().getJsonObject(index);
              String name = doc.getString("name");
              String fileId = doc.getString("fileId");
              Long lastModified = doc.getLong("lastModified");

              JsonObject message = JsonObject.of("name", name);

              DMS.downloadDmsDocsCheck(name, lastModified)
                  .compose(checkResult -> {
                    switch (checkResult) {
                      case "SKIP":
                        MSG.sendToUser(userName, message.put("msg", "DMS_DOWNLOAD_SKIP").encode());
                        break;
                      case "START":
                        MSG.sendToUser(userName, message.put("msg", "DMS_DOWNLOAD_START").encode());
                        break;
                      case "REDOWNLOAD":
                        MSG.sendToUser(userName, message.put("msg", "DMS_DOWNLOAD_REDOWNLOAD").encode());
                        break;
                    }
                    return Future.succeededFuture(checkResult);
                  })
                  .andThen(checkResult -> {
                    if (checkResult.result().equals("START") || checkResult.result().equals("REDOWNLOAD")) {
                      DMS.downloadDmsDocs(name, fileId, lastModified)
                          .onSuccess(res -> {
                            switch (res) {
                              case "DOWNLOADED":
                                MSG.sendToUser(userName,
                                    message.put("msg", "DMS_DOWNLOAD_SUCCESS").encode());
                                break;
                            }
                          })
                          .onFailure(err -> {
                            MSG.sendToUser(userName,
                                message.put("msg", "DMS_DOWNLOAD_FAILURE").encode());
                          });
                    }
                  });

              return Future.succeededFuture();
            });
          }
        });
  }
}