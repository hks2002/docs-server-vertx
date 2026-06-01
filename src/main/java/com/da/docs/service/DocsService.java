/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-04-02 16:48:46                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-25 16:02:11                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.da.docs.serviceStatic.DB;
import com.da.docs.serviceStatic.FS;
import com.da.docs.serviceStatic.MSG;

import io.vertx.core.Future;
import io.vertx.core.file.FileProps;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DocsService {
  public Future<Integer> addDocs(JsonObject obj) {
    return DB.insertByFile("insertDocs", obj)
        .onFailure(err -> {
          log.error("[File] Add doc failed: {}\n{}", obj.encodePrettily(), err.getCause());
        })
        .onSuccess(rst -> {
          log.trace("[File] Add doc: {}", obj.encodePrettily());
          LogService.addLog(
              "DOC_INFO_CREATE",
              "127.0.0.1",
              "system",
              "",
              obj.getString("file_name"));
        });
  }

  public Future<Integer> modifyDocs(JsonObject obj) {
    return DB.updateByFile("updateDocs", obj)
        .onFailure(err -> {
          log.error("[File] Modify doc failed: {}\n{}", obj.encodePrettily(), err.getCause());
        })
        .onSuccess(ar3 -> {
          log.trace("[File] Modify doc: {}", obj.encodePrettily());
          LogService.addLog(
              "DOC_INFO_UPDATE",
              "127.0.0.1",
              "system",
              "",
              obj.getString("file_name"));
        });
  }

  public Future<Integer> removeDocs(JsonObject obj) {
    return DB.deleteByFile("deleteDocs", obj)
        .onFailure(err -> {
          log.error("[File] Delete doc from Database failed: {}\n{}", err.getCause(), obj.encodePrettily());
        })
        .onSuccess(rst -> {
          log.trace("[File] Delete doc from Database: {}", obj.encodePrettily());
          LogService.addLog(
              "DOC_INFO_DELETE",
              "127.0.0.1",
              "system",
              "",
              obj.getString("file_name"));
        });
  }

  public Future<Integer> removeDBDuplicateDocsByName() {
    return DB.deleteByFile("deleteDuplicateDocsByName", new JsonObject())
        .onFailure(err -> {
          log.error("[File] Delete duplicate doc by name from Database failed: {}", err.getCause());
        })
        .onSuccess(rst -> {
          log.trace("[File] Delete duplicate doc by name from Database successful");
        });
  }

  /**
   * add or modify docs by file name, if docs exist, modify it, else add it
   *
   * @param fileName
   * @param size
   * @param is_link
   * @param dms_id
   * @param md5
   * @param docCreateAt
   * @param docModifiedAt
   * @return
   */
  public Future<Integer> addOrModifyDocs(
      String fileName,
      long size,
      boolean is_link,
      String dms_id,
      String md5,
      String docCreateAt,
      String docModifiedAt) {
    JsonObject doc = JsonObject.of(
        "file_name", fileName,
        "size", size,
        "is_link", is_link,
        "dms_id", dms_id,
        "md5", md5,
        "doc_create_at", docCreateAt,
        "doc_modified_at", docModifiedAt);

    return searchDocsByName(JsonObject.of("file_name", fileName))
        .compose(fileList -> {
          if (fileList.size() == 0) {
            return addDocs(doc);
          } else if (fileList.size() == 1) {
            JsonObject oldDocs = fileList.get(0).copy();
            oldDocs.put("md5", md5);
            oldDocs.put("size", size);
            oldDocs.put("is_link", is_link);
            if (dms_id != null && !dms_id.isEmpty()) {
              oldDocs.put("dms_id", dms_id);
            }
            oldDocs.put("doc_create_at", docCreateAt);
            oldDocs.put("doc_modified_at", docModifiedAt);
            return modifyDocs(oldDocs);
          } else {
            // here seems never happens, fileName is unique
            List<Future<Integer>> f1 = new ArrayList<>();

            for (int i = 0; i < fileList.size(); i++) {
              JsonObject oldDocs = fileList.get(i).copy();
              if (i == 0) {
                oldDocs.put("is_link", is_link);
                if (dms_id != null && !dms_id.isEmpty()) {
                  oldDocs.put("dms_id", dms_id);
                }
                oldDocs.put("size", size);
                oldDocs.put("dms_id", dms_id);
                oldDocs.put("doc_create_at", docCreateAt);
                oldDocs.put("doc_modified_at", docModifiedAt);
                f1.add(modifyDocs(oldDocs));
              } else {
                f1.add(removeDocs(oldDocs));
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

  public Future<List<JsonObject>> searchDocs(JsonObject obj) {
    return DB.queryByFile("queryDocs", obj)
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              obj.getString("file_name"),
              err.getCause());
        });
  }

  public Future<List<JsonObject>> searchDuplicatedDocs() {
    return DB.queryByFile("queryDuplicateDocs", JsonObject.of())
        .onFailure(err -> {
          log.error("[File] Failed to search duplicated docs: {}",
              err.getCause());
        });
  }

  public Future<List<JsonObject>> searchDocsByName(JsonObject obj) {
    return DB.queryByFile("queryDocsByName", obj)
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              obj.getString("file_name"),
              err.getCause());
        });
  }

  public Future<List<JsonObject>> searchDocsFromTLSOLDByName(JsonObject obj) {
    return DB.queryByFile("queryDocsFromTLSOLDByName", obj, 1)
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              obj.getString("file_name"),
              err.getCause());
        });
  }

  public Future<List<JsonObject>> searchDocsByMD5(JsonObject obj) {
    return DB.queryByFile("queryDocsByMD5", obj)
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              obj.getString("md5"),
              err.getCause());
        });
  }

  /**
   * Build file info and add to database
   * <p>
   *
   * @param fileName the destination file name
   * @param id       dms file id
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note The file must already exist in the file system
   */
  public Future<Integer> addOrModifyFileInfo(String fileName, String id) {
    String docRoot = FS.getDocsRoot();
    String toSubFolder = FS.getFolderPathByFileName(fileName);
    String toFileFullPath = docRoot + '/' + toSubFolder + '/' + fileName;
    log.debug(" [File] Add file info: {}", toFileFullPath);

    return FS.isFileExists(toFileFullPath)
        .compose(fileExists -> {
          if (!fileExists) {
            log.error("[File][Add] Destination file is not a file: {}", toFileFullPath);
            return null;
          }

          Future<String> f3 = FS.computerMd5(toFileFullPath);
          Future<FileProps> f4 = FS.fs.props(toFileFullPath);

          return Future.all(f3, f4)
              .compose(data2 -> {
                String md5 = data2.resultAt(0);
                FileProps props = data2.resultAt(1);

                long size = props.size();
                long creationTime = props.creationTime();
                long modifiedTime = props.lastModifiedTime();
                LocalDateTime createAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(creationTime),
                    ZoneId.systemDefault());
                LocalDateTime modifiedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(modifiedTime),
                    ZoneId.systemDefault());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String createAtStr = createAt.format(formatter);
                String modifiedAtStr = modifiedAt.format(formatter);

                return addOrModifyDocs(fileName, size, false, id, md5, createAtStr, modifiedAtStr);
              });

        });
  }

  /**
   * Delete documents in database that do not exist in the file system
   * 
   *
   * @param targetFolder the docs store directory, absolute path
   */
  public Future<Object> cleanNonExistsDocs(String username) {
    return searchDocs(JsonObject.of("limit", 999999))
        .compose(fileList -> {
          log.info("\u001b[35m Clean NonExists files in db");
          log.info("[Clean] Found {} files in database to check", fileList.size());

          MSG.sendToUser(username, JsonObject.of(
              "msg", "CLEAN_NON_EXISTS_START",
              "total", fileList.size(),
              "processed", 0).encode());

          String docRoot = FS.getDocsRoot();
          AtomicInteger deletedCount = new AtomicInteger(0);

          List<Future<Boolean>> checkFutures = new ArrayList<>();
          for (int i = 0; i < fileList.size(); i++) {
            Integer currentIndex = i;
            JsonObject currentFile = fileList.get(i);
            String fileName = currentFile.getString("file_name");
            String toSubFolder = FS.getFolderPathByFileName(fileName);
            String file = docRoot + '/' + toSubFolder + '/' + fileName;

            Future<Boolean> checkFuture = FS.fs.exists(file)
                .compose(exists -> {
                  if (!exists) {
                    deletedCount.incrementAndGet();
                    log.info("[Clean] Delete non-existent file from Database: {}", file);
                    return removeDocs(currentFile).compose(res -> Future.succeededFuture(true));
                  }
                  log.debug("[Clean] {}/{} Find file from Database: {}", currentIndex, fileList.size(), file);
                  return Future.succeededFuture(true);
                })
                .recover(err -> {
                  log.error("[Clean] Failed to check file existence: {}, error: {}", file, err.getCause());
                  return Future.succeededFuture(true);
                })
                .eventually(() -> {
                  int processed = deletedCount.incrementAndGet();
                  if (processed % 10 == 0 || processed == fileList.size()) {
                    MSG.sendToUser(username, JsonObject.of(
                        "msg", "CLEAN_NON_EXISTS_PROCESS",
                        "total", fileList.size(),
                        "processed", processed)
                        .encode());
                  }
                  return Future.succeededFuture();
                });

            checkFutures.add(checkFuture);
          }

          return Future.join(checkFutures).onComplete(v -> {
            log.info("[Clean] Completed check. Total files: {}, Non-existent files: {}",
                fileList.size(), deletedCount.get());
            MSG.sendToUser(username, JsonObject.of(
                "msg", "CLEAN_NON_EXISTS_END",
                "total", fileList.size(),
                "processed", deletedCount.get())
                .encode());
          }).mapEmpty();
        });

  }

  /**
   * Link documents in database that are duplicated in the file system
   * <p>
   *
   * @param targetFolder the docs store directory, absolute path
   */
  public Future<Object> cleanDuplicatedDocs(String username) {

    return searchDuplicatedDocs()
        .compose(fileList -> {
          log.info("\u001b[35m Clean duplicated files in db");
          log.info("[Clean] Found {} files in database to link", fileList.size());
          MSG.sendToUser(username, JsonObject.of(
              "msg", "CLEAN_DUPLICATE_START",
              "total", fileList.size(),
              "processed", 0).encode());

          String docRoot = FS.getDocsRoot();
          AtomicInteger linkCount = new AtomicInteger(0);

          List<Future<Void>> linkFutures = new ArrayList<>();
          for (int i = 0; i < fileList.size(); i++) {
            Integer currentIndex = i;
            JsonObject currentFile = fileList.get(i);

            String oriFileName = currentFile.getString("original_file_name");
            String oriToSubFolder = FS.getFolderPathByFileName(oriFileName);
            String dupFileName = currentFile.getString("dup_file_name");
            String dupToSubFolder = FS.getFolderPathByFileName(dupFileName);
            String oriFile = docRoot + '/' + oriToSubFolder + '/' + oriFileName;
            String dupFileLink = docRoot + '/' + dupToSubFolder + '/' + dupFileName;

            Future<Boolean> f1 = FS.fs.exists(oriFile);
            Future<Boolean> f2 = FS.fs.exists(dupFileLink);

            Future<Void> linkFuture = Future.all(f1, f2).compose(data -> {
              Boolean oriFileExists = data.resultAt(0);
              Boolean dupFileExists = data.resultAt(1);

              if (!oriFileExists) {
                log.error("[Clean] Link target not exists {}", oriFile);
                return Future.succeededFuture();
              }

              if (dupFileExists) {
                return FS.safeLink(dupFileLink, oriFile)
                    .compose(res1 -> {
                      log.info("[Clean] {}/{} Link {} to {}", currentIndex, fileList.size(), dupFileLink, oriFile);

                      int processed = linkCount.incrementAndGet();
                      if (processed % 10 == 0 || processed == fileList.size()) {
                        MSG.sendToUser(username, JsonObject.of(
                            "msg", "CLEAN_DUPLICATE_PROCESS",
                            "total", fileList.size(),
                            "processed", processed)
                            .encode());
                      }
                      return Future.succeededFuture();
                    });
              } else {
                // here in normal case, should not happen
                log.warn("[Clean] Link {} failed, dup file not exists", dupFileLink);

                return FS.fs.link(dupFileLink, oriFile)
                    .compose(res1 -> {
                      log.info("[Clean] {}/{} Link {} to {}", currentIndex, fileList.size(), dupFileLink, oriFile);

                      int processed = linkCount.incrementAndGet();
                      if (processed % 10 == 0 || processed == fileList.size()) {
                        MSG.sendToUser(username, JsonObject.of(
                            "msg", "CLEAN_DUPLICATE_PROCESS",
                            "total", fileList.size(),
                            "processed", processed)
                            .encode());
                      }
                      return Future.succeededFuture();
                    });
              }
            });

            linkFutures.add(linkFuture);
          }

          return Future.join(linkFutures).onComplete(v -> {
            log.info("[Clean] Completed check. Total files: {}, linked files: {}", fileList.size(), linkCount.get());
            MSG.sendToUser(username, JsonObject.of(
                "msg", "CLEAN_DUPLICATE_END",
                "total", fileList.size(),
                "processed", linkCount.get())
                .encode());
          }).mapEmpty();
        });
  }
}