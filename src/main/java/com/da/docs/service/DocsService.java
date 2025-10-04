/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-04-02 16:48:46                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-10-04 15:16:50                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs.service;

import java.util.ArrayList;
import java.util.List;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DocsService {
  public Future<Object> addDocs(JsonObject obj) {
    return DB.insertByFile("insertDocs", obj);
  }

  /**
   * add or modify docs by file name, if docs exist, modify it, else add it
   * 
   * @param fileName
   * @param size
   * @param location
   * @param dms_id
   * @param md5
   * @param docCreateAt
   * @param docModifiedAt
   * @return
   */
  public Future<Object> addDocs(
      String fileName,
      long size,
      String location,
      String dms_id,
      String md5,
      String docCreateAt,
      String docModifiedAt) {
    JsonObject doc = JsonObject.of(
        "file_name", fileName,
        "size", size,
        "location", location,
        "dms_id", dms_id,
        "md5", md5,
        "doc_create_at", docCreateAt,
        "doc_modified_at", docModifiedAt);

    return searchDocsByName(doc)
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              fileName,
              err.getMessage());
        })
        .compose(fileList -> {
          log.trace("[File] docs by file name {}: {}", fileName, fileList.size());

          if (fileList.size() == 0) {
            return addDocs(doc)
                .onFailure(err -> {
                  log.error("[File] Add doc failed: {}", fileName, err.getMessage());
                })
                .onSuccess(ar3 -> {
                  log.trace("[File] Add doc: {}", fileName);
                  LogService.addLog("DOC_INFO_CREATE", "127.0.0.1", "system", "system", fileName, location, md5);
                })
                .compose(ar3 -> {
                  return Future.succeededFuture();
                });

          } else if (fileList.size() == 1) {
            JsonObject oldDocs = fileList.get(0);
            oldDocs.put("location", location);
            oldDocs.put("md5", md5);

            return modifyDocs(oldDocs)
                .onFailure(err -> {
                  log.error("[File] Modify doc failed: {}", fileName, err.getMessage());
                })
                .onSuccess(ar3 -> {
                  log.trace("[File] Modify doc: {}", fileName);
                  LogService.addLog("DOC_INFO_UPDATE", "127.0.0.1", "system", "system", fileName, location, md5);
                })
                .compose(ar3 -> {
                  return Future.succeededFuture();
                });
          } else {
            List<Future<Object>> f1 = new ArrayList<>();

            for (int i = 0; i < fileList.size(); i++) {
              JsonObject oldDocs = fileList.get(i);
              if (i == 0) {
                oldDocs.put("location", location);
                f1.add(modifyDocs(oldDocs)
                    .onFailure(err -> {
                      log.error("[File] Modify doc failed: {} {}", fileName, err.getMessage());
                    })
                    .onSuccess(ar -> {
                      log.trace("[File] Modify doc: {}", fileName);
                      LogService.addLog("DOC_INFO_UPDATE", "127.0.0.1", "system", "system", fileName, md5);
                    }));
              } else {
                f1.add(removeDocs(oldDocs)
                    .onFailure(err -> {
                      log.error("[File] Remove doc failed: {} {}", fileName, err.getMessage());
                    })
                    .onSuccess(ar -> {
                      log.trace("[File] Remove doc: {}", fileName);
                      LogService.addLog("DOC_INFO_DELETE", "127.0.0.1", "system", "system", fileName, md5);
                    }));
              }
            }

            return Future.all(f1)
                .compose(ar -> {
                  if (ar.succeeded()) {
                    return Future.succeededFuture();
                  } else {
                    return Future.failedFuture("");
                  }
                });

          }
        });
  }

  public Future<Object> modifyDocs(JsonObject obj) {
    return DB.updateByFile("updateDocs", obj);
  }

  public Future<Object> removeDocs(JsonObject obj) {
    return DB.updateByFile("deleteDocs", obj);
  }

  public Future<Object> removeDuplicateDocsByName() {
    return DB.updateByFile("deleteDuplicateDocsByName", new JsonObject());
  }

  /**
   * Delete documents in database that do not exist in the file system
   * <p>
   *
   * @param fs           the file system
   * @param targetFolder the docs store directory, absolute path
   */
  public static Future<Void> cleanDBDoc(FileSystem fs, String targetFolder) {
    if (!fs.existsBlocking(targetFolder) || !fs.propsBlocking(targetFolder).isDirectory()) {
      log.error("[Clean] Destination path is not a directory: {}", targetFolder);
      return Future.failedFuture("Destination path is not a directory: " + targetFolder);
    }

    DocsService docsService = new DocsService();
    docsService.searchDocs(JsonObject.of("limit", 999999999)).onSuccess(fileList -> {
      log.info("\u001b[35m Clean invalid files in db");

      for (int i = 0; i < fileList.size(); i++) {
        String file = targetFolder + '/' + fileList.get(i).getString("location");
        if (!fs.existsBlocking(file)) {
          log.info("[Clean] Delete no exist file: {}", file);
          docsService.removeDocs(fileList.get(i))
              .onFailure(err -> {
                log.error("[Clean] Delete doc failed: {}", file, err.getMessage());
              })
              .onSuccess(rst -> {
                log.info("[Clean] Delete doc: {}", file);
              });
        }
      }

    }).andThen(a -> {
      log.info("\u001b[35m Clean invalid files in db");
      docsService.removeDuplicateDocsByName();
    }

    );
    return Future.succeededFuture();
  }

  public Future<List<JsonObject>> searchDocs(JsonObject obj) {
    return DB.queryByFile("queryDocs", obj);
  }

  public Future<List<JsonObject>> searchDocsByName(JsonObject obj) {
    return DB.queryByFile("queryDocsByName", obj);
  }

  public Future<List<JsonObject>> searchDocsFromTLSOLDByName(JsonObject obj) {
    return DB.queryByFile("queryDocsFromTLSOLDByName", obj, 1);
  }

  public Future<List<JsonObject>> searchDocsByMD5(JsonObject obj) {
    return DB.queryByFile("queryDocsByMD5", obj);
  }

}
