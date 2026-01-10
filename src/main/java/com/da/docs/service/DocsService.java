/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-04-02 16:48:46                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 17:05:35                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.da.docs.VertxApp;
import com.da.docs.db.DB;
import com.da.docs.utils.FSUtils;

import io.vertx.core.Future;
import io.vertx.core.file.FileProps;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DocsService {
  public Future<Integer> addDocs(JsonObject obj) {
    return DB.insertByFile("insertDocs", obj)
        .onFailure(err -> {
          log.error("[File] Add doc failed: {}\n{}", obj.encodePrettily(), err.getMessage());
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
          log.error("[File] Modify doc failed: {}\n{}", obj.encodePrettily(), err.getMessage());
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
          log.error("[File] Delete doc from Database failed: {}\n{}", err.getMessage(), obj.encodePrettily());
        }).onSuccess(rst -> {
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
          log.error("[File] Delete duplicate doc by name from Database failed: {}", err.getMessage());
        }).onSuccess(rst -> {
          log.trace("[File] Delete duplicate doc by name from Database successful");
        });

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
  public Future<Integer> addDocs(
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

    return searchDocsByName(JsonObject.of("file_name", fileName))
        .compose(fileList -> {
          if (fileList.size() == 0) {
            return addDocs(doc);
          } else if (fileList.size() == 1) {
            JsonObject oldDocs = fileList.get(0).copy();
            oldDocs.put("location", location);
            oldDocs.put("md5", md5);
            oldDocs.put("size", size);
            oldDocs.put("dms_id", dms_id);
            oldDocs.put("doc_create_at", docCreateAt);
            oldDocs.put("doc_modified_at", docModifiedAt);
            return modifyDocs(oldDocs);
          } else {
            List<Future<Integer>> f1 = new ArrayList<>();

            for (int i = 0; i < fileList.size(); i++) {
              JsonObject oldDocs = fileList.get(i).copy();
              if (i == 0) {
                oldDocs.put("location", location);
                oldDocs.put("location", location);
                oldDocs.put("md5", md5);
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
    return DB.queryByFile("queryDocs", obj).onFailure(err -> {
      log.error("[File] Failed to search docs by file name {}: {}",
          obj.getString("file_name"),
          err.getMessage());
    });
  }

  public Future<List<JsonObject>> searchDocsByName(JsonObject obj) {
    return DB.queryByFile("queryDocsByName", obj)
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              obj.getString("file_name"),
              err.getMessage());
        });
  }

  public Future<List<JsonObject>> searchDocsFromTLSOLDByName(JsonObject obj) {
    return DB.queryByFile("queryDocsFromTLSOLDByName", obj, 1).onFailure(err -> {
      log.error("[File] Failed to search docs by file name {}: {}",
          obj.getString("file_name"),
          err.getMessage());
    });
  }

  public Future<List<JsonObject>> searchDocsByMD5(JsonObject obj) {
    return DB.queryByFile("queryDocsByMD5", obj).onFailure(err -> {
      log.error("[File] Failed to search docs by file name {}: {}",
          obj.getString("md5"),
          err.getMessage());
    });
  }

  /**
   * Delete documents in database that do not exist in the file system
   * <p>
   *
   * @param targetFolder the docs store directory, absolute path
   */
  public void cleanDBNonExistsDocs(String targetFolder) {
    if (!VertxApp.fs.existsBlocking(targetFolder) || !VertxApp.fs.propsBlocking(targetFolder).isDirectory()) {
      log.error("[Clean] Destination path is not a directory: {}", targetFolder);
      return;
    }

    DocsService docsService = new DocsService();
    docsService.searchDocs(JsonObject.of("limit", 999999999))
        .onSuccess(fileList -> {
          log.info("\u001b[35m Clean invalid files in db");

          for (int i = 0; i < fileList.size(); i++) {
            String file = targetFolder + '/' + fileList.get(i).getString("location");
            if (!VertxApp.fs.existsBlocking(file)) {
              log.info("[Clean] Delete non-existent file from Database: {}", file);
              docsService.removeDocs(fileList.get(i));
            }
          }
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
  public Future<Integer> addFileInfo(String fileName, String id) {

    return Future.all(FSUtils.getDocsRoot(), FSUtils.getFolderPathByFileName(fileName)).compose(d1 -> {
      String docRoot = d1.resultAt(0);
      String toSubFolder = d1.resultAt(1);
      String toFileFullPath = docRoot + '/' + toSubFolder + '/' + fileName;
      log.debug(" [File] Add file info: {}", toFileFullPath);

      return FSUtils.isFileExists(toFileFullPath).compose(fileExists -> {
        if (!fileExists) {
          log.error("[File][Add] Destination file is not a file: {}", toFileFullPath);
          return null;
        }

        return Future.all(
            FSUtils.computerMd5(toFileFullPath),
            FSUtils.fs.props(toFileFullPath))
            .compose(data -> {
              String md5 = data.resultAt(0);
              FileProps props = data.resultAt(1);

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

              DocsService docsService = new DocsService();
              return docsService.addDocs(fileName, size, toSubFolder, id, md5, createAtStr, modifiedAtStr);
            });

      });
    });

  }

  /**
   * Move file to target folder
   * <p>
   *
   * @param fromFileFullPath the source file, absolute path
   * @param toFileName       the destination file name
   * @param mode             the mode, MOVE or COPY or INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public Future<Void> moveFile(String fromFileFullPath, String toFileName, String mode) {
    Future<Void> f1 = FSUtils.isFileExists(fromFileFullPath).compose(sourceFileExists -> {
      if (!sourceFileExists) {
        log.error("[File][Move] Source file is not a file: {}", fromFileFullPath);
        return Future.failedFuture("Source file is not a file: " + fromFileFullPath);
      }
      ;
      return Future.succeededFuture();
    });

    Future<String> f2 = FSUtils.getDocsRoot();
    Future<String> f3 = FSUtils.getFolderPathByFileName(toFileName);

    Future<String> f4 = Future.all(f1, f2, f3).compose(data -> {
      String rootDir = data.resultAt(1);
      String toSubFolder = data.resultAt(2);
      String toFolderFullPath = rootDir + '/' + toSubFolder;

      return FSUtils.fs.exists(toFolderFullPath).compose(toFolderExists -> {
        if (!toFolderExists) {
          log.trace("[File] Make dir: {}", toFolderFullPath);
          return FSUtils.fs.mkdirs(toFolderFullPath).compose(success -> {
            return Future.succeededFuture(toFolderFullPath);
          });
        }
        return Future.succeededFuture(toFolderFullPath);
      });
    });

    return f4.compose(toFolderFullPath -> {
      String toFileFullPath = toFolderFullPath + '/' + toFileName;
      return FSUtils.isFileExists(toFileFullPath).compose(fileExists -> {
        if (fileExists) {
          log.warn("[File][Move] File already exists: {}", toFileFullPath);
        }

        return moveFileCore(fromFileFullPath, toFileFullPath, mode, fileExists);
      });
    });
  }

  /**
   * Move file to target folder
   * <p>
   *
   * @param fromFileFullPath the source file, absolute path
   * @param toFileFullPath   the destination absolute path
   * @param mode             the mode, MOVE or COPY or INFO
   * @param fileExists       the file exists or not
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public Future<Void> moveFileCore(String fromFileFullPath, String toFileFullPath, String mode, boolean fileExists) {

    Future<Void> f_moveOrCopy = null;
    switch (mode) {
      case "COPY":
        f_moveOrCopy = FSUtils.fs.copy(fromFileFullPath, toFileFullPath);
        break;
      case "MOVE":
        f_moveOrCopy = FSUtils.fs.move(fromFileFullPath, toFileFullPath);
        break;
      case "UPDATE":
        f_moveOrCopy = fileExists
            ? FSUtils.fs.delete(toFileFullPath)
                .compose(ar -> {
                  return FSUtils.fs.move(fromFileFullPath, toFileFullPath);
                })
            : FSUtils.fs.move(fromFileFullPath, toFileFullPath);
        break;
      default:
        log.error("[File] Unknown mode: {}", mode);
        return Future.failedFuture("Unknown mode: " + mode);
    }

    return f_moveOrCopy
        .onFailure(err -> {
          log.error("[File] Failed {} file {} to {}: {}",
              mode,
              fromFileFullPath,
              toFileFullPath,
              err.getMessage());
        })
        .onSuccess(ar2 -> {
          log.trace("[File] Success {} file {} to {}",
              mode,
              fromFileFullPath,
              toFileFullPath);
        });
  }

  /**
   * Move files from one directory to another, ignore hidden files
   * <p>
   *
   * @param fromFolder the source directory, absolute path
   * @param mode       the mode to use, COPY or MOVE, INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public Future<Void> buildFolderInfo(String fromFolder, String mode) {

    return FSUtils.fs.props(fromFolder).compose(props -> {
      if (!props.isDirectory()) {
        log.error("[Folders] Source path is not a directory: {}", fromFolder);
        return Future.failedFuture("Source path is not a directory: " + fromFolder);
      }
      return FSUtils.fs.readDir(fromFolder);
    }).compose(fileList -> {
      try {
        for (int i = 0; i < fileList.size(); i++) {
          String fileInFolder = fileList.get(i);
          FileProps inProps = VertxApp.fs.propsBlocking(fileInFolder);

          if (inProps.isDirectory()) {
            log.info("[Folders][INFO_BUILD] {}", fileInFolder);
            buildFolderInfo(fileInFolder, mode).onSuccess(rst -> {
              // delete the empty folder
              // fs.delete(fileInFolder);
            });

          } else if (inProps.isRegularFile()) {
            // get the file name, without the extension
            File file = new File(fileInFolder);
            String fileName = file.getName();

            // skip hidden files
            if (fileName.endsWith(".prt") ||
                fileName.startsWith("~") ||
                fileName.startsWith("$") ||
                fileName.toLowerCase().equals("thumbs.db")) {
              continue;
            }

            log.info("[File][INFO_BUILD] {}", fileInFolder);
            switch (mode) {
              case "COPY":
              case "MOVE":
                moveFile(fileInFolder, fileName, mode)
                    .onFailure(err -> {
                      throw new RuntimeException(
                          "RuntimeException: Failed to " + mode + " file " + fileInFolder + ": " + err.getMessage());
                    }).onSuccess(ar2 -> {
                      addFileInfo(fileName, "");
                    });
                break;
              case "INFO":
                addFileInfo(fileName, null);
                break;
              default:
                log.error("[File] Unknown mode: {}", mode);
                return Future.failedFuture("Unknown mode: " + mode);
            }

          } else {
            // do nothing
          }
        }
        return Future.succeededFuture();

      } catch (Exception e) {
        log.error("[Folders] Error: {}", e.getMessage());
        return Future.failedFuture(e);
      }
    });

  }

}
