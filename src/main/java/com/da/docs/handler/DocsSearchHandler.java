/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:30:12                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import java.util.ArrayList;
import java.util.List;

import com.da.docs.annotation.GetMapping;
import com.da.docs.annotation.Permission;
import com.da.docs.service.DocsService;
import com.da.docs.serviceStatic.FS;

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
@GetMapping("/docs-api/searchDocs")
@Permission("DOCS_READ")
public class DocsSearchHandler implements Handler<RoutingContext> {
  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    String PN = request.getParam("PN");
    DocsService docsService = new DocsService();

    docsService.searchDocsByName(JsonObject.of("file_name", "%" + PN + "%"))
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
          response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json").end("[]");
        })
        .compose(list -> {
          List<Future<JsonObject>> futures = new ArrayList<>();

          for (JsonObject f : list) {
            String fileName = f.getString("file_name");

            String docRoot = FS.getDocsRoot();
            String toFolderPath = FS.getFolderPathByFileName(fileName);
            String toFileFullPath = docRoot + '/' + toFolderPath + '/' + fileName;
            log.debug("toFileFullPath: {}", toFileFullPath);

            Future<JsonObject> futureDoc = FS.fs.props(toFileFullPath)
                .compose(props -> {
                  // make sure the info is correct based on the file,
                  // not the database,database may be wrong
                  JsonObject o = new JsonObject();
                  o.put("name", fileName);
                  o.put("size", props.size());
                  o.put("lastModified", props.lastModifiedTime());
                  o.put("url", "/docs-api/docs/" + toFolderPath + '/' + fileName);
                  o.put("fileId", f.getInteger("dms_id", 0));

                  // for updating the doc info
                  f.put("size", props.size());
                  f.put("lastModified", props.lastModifiedTime());
                  docsService.modifyDocs(f);

                  return Future.succeededFuture(o);
                })
                .onFailure(e -> {
                  log.error("File not found:{}", fileName);
                });

            futures.add(futureDoc);
          } // end of for list

          return Future.join(futures)
              .map(cf -> {
                List<JsonObject> result = new ArrayList<>();
                for (int i = 0; i < cf.size(); i++) {
                  JsonObject o = cf.resultAt(i);
                  if (o != null)
                    result.add(o);
                }
                return result;
              });
        })
        .onSuccess(resultList -> {
          response
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
              .end(resultList.toString());
        })
        .onFailure(ar -> {
          log.error("search docs failed {}", ar.getCause());
          response
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
              .end("[]");
        });

  }
}