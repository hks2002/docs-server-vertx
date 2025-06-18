/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-06 17:22:33                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Hex;

import com.da.docs.service.DocsService;
import com.da.docs.service.LogService;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlResult;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FSUtils {

  private static String computerMd5(FileSystem fs, String fileName) {
    Buffer buf = fs.readFileBlocking(fileName);
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] bytes = buf.getBytes();
      md.update(bytes);
      byte[] digest = md.digest();

      return new String(Hex.encode(digest));
    } catch (Exception e) {
      log.error("Failed to compute MD5 for file {}: {}", fileName, e.getMessage());
      return "";
    }
  }

  public static void updateFileModifiedDate(String filePath, long timestamp) {
    try {
      Path path = Path.of(filePath);
      FileTime fileTime = FileTime.from(Instant.ofEpochMilli(timestamp));
      Files.setLastModifiedTime(path, fileTime);
      log.info("Successfully updated the modified date for file: {}", filePath);
    } catch (Exception e) {
      log.error("Failed to update the modified date for file: {}", filePath, e);
    }
  }

  public static void updateFileModifiedDate(String filePath, String timestamp) {
    try {
      updateFileModifiedDate(filePath, Long.parseLong(timestamp));
    } catch (Exception e) {
      log.error("Failed parse long from String", timestamp);
    }
  }

  /**
   * Add document to database, same md5 will update the location and delete the
   * old file
   * <p>
   *
   * @param docs the document to add
   *
   * @Note this method does not move or copy the file
   */
  public static Future<Void> addDoc(JsonObject docs) {
    LogService logService = new LogService();
    DocsService docsService = new DocsService();

    String toFileName = docs.getString("file_name");
    String toLocation = docs.getString("location");
    String md5 = docs.getString("md5");

    return docsService.searchDocsByName(JsonObject.of("file_name", toFileName))
        .onFailure(err -> {
          log.error("[File] Failed to search docs by file name {}: {}",
              toFileName,
              err.getMessage());
        })
        .onSuccess(fileList -> {
          log.trace("[File] docs by file name {}: {}", toFileName, fileList.size());
        })
        .compose(fileList -> {

          if (fileList.size() == 0) {
            return docsService.addDocs(docs)
                .onFailure(err -> {
                  log.error("[File] Add doc failed: {}", toFileName, err.getMessage());
                })
                .onSuccess(ar3 -> {
                  log.trace("[File] Add doc: {}", toFileName);
                  logService.addLog("DOC_INFO_CREATE", "127.0.0.1", "system", "system", toFileName, toLocation, md5);
                })
                .compose(ar3 -> {
                  return Future.succeededFuture();
                });

          } else if (fileList.size() == 1) {
            JsonObject oldDocs = fileList.get(0);
            oldDocs.put("location", toLocation);
            oldDocs.put("md5", md5);

            return docsService.modifyDocs(oldDocs)
                .onFailure(err -> {
                  log.error("[File] Modify doc failed: {}", toFileName, err.getMessage());
                })
                .onSuccess(ar3 -> {
                  log.trace("[File] Modify doc: {}", toFileName);
                  logService.addLog("DOC_INFO_UPDATE", "127.0.0.1", "system", "system", toFileName, toLocation, md5);
                })
                .compose(ar3 -> {
                  return Future.succeededFuture();
                });
          } else {
            List<Future<SqlResult<Void>>> f1 = new ArrayList<>();

            for (int i = 0; i < fileList.size(); i++) {
              JsonObject oldDocs = fileList.get(i);
              if (i == 0) {
                oldDocs.put("location", toLocation);
                f1.add(docsService.modifyDocs(oldDocs)
                    .onFailure(err -> {
                      log.error("[File] Modify doc failed: {} {}", toFileName, err.getMessage());
                    })
                    .onSuccess(ar -> {
                      log.trace("[File] Modify doc: {}", toFileName);
                      logService.addLog("DOC_INFO_UPDATE", "127.0.0.1", "system", "system", toFileName, md5);
                    }));
              } else {
                f1.add(docsService.removeDocs(oldDocs)
                    .onFailure(err -> {
                      log.error("[File] Remove doc failed: {} {}", toFileName, err.getMessage());
                    })
                    .onSuccess(ar -> {
                      log.trace("[File] Remove doc: {}", toFileName);
                      logService.addLog("DOC_INFO_DELETE", "127.0.0.1", "system", "system", toFileName, md5);
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

  /**
   * Move file to target folder
   * <p>
   *
   * @param fs               the file system
   * @param fromFileFullPath the source file, absolute path
   * @param toFileName       the destination file name
   * @param toFolder         the destination directory, absolute path
   * @param toSubFolderDeep  the depth of sub-folders to create
   * @param toSubFolderLen   the length of each sub-folder
   * @param mode             the mode, MOVE or COPY or INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public static Future<Void> setFileInfo(
      FileSystem fs,
      String fromFileFullPath,
      String toFileName,
      String toFolder,
      int toSubFolderDeep,
      int toSubFolderLen,
      String mode) {

    if (!fs.existsBlocking(fromFileFullPath) || fs.propsBlocking(fromFileFullPath).isDirectory()) {
      log.error("[File] Source file is not a file: {}", fromFileFullPath);
      return Future.failedFuture("");
    }

    if (!fs.existsBlocking(toFolder) || !fs.propsBlocking(toFolder).isDirectory()) {
      log.error("[File] Destination path is not a directory: {}", toFolder);
      return Future.failedFuture("");
    }

    try {
      FileProps inProps = fs.propsBlocking(fromFileFullPath);
      // get document properties
      JsonObject docs = new JsonObject();

      // get the destination folder
      String toSubFolder = CommonUtils.getPathByFileName(toFileName, toSubFolderDeep, toSubFolderLen);
      String toFolderFullPath = toFolder + '/' + toSubFolder;
      String toLocation = toSubFolder + '/' + toFileName;
      String md5 = FSUtils.computerMd5(fs, fromFileFullPath);

      docs.put("file_name", toFileName);
      docs.put("size", inProps.size());
      docs.put("location", toLocation);
      docs.put("md5", md5);
      LocalDateTime createAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(inProps.creationTime()),
          ZoneId.systemDefault());
      LocalDateTime modifiedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(inProps.lastModifiedTime()),
          ZoneId.systemDefault());

      docs.put("doc_create_at", createAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
      docs.put("doc_modified_at", modifiedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

      // make sure toFolder exists
      if (!fs.existsBlocking(toFolderFullPath)) {
        log.trace("[File] Make dir: {}", toFolderFullPath);
        fs.mkdirsBlocking(toFolderFullPath);
      }
      String toFileFullPath = toFolderFullPath + '/' + toFileName;
      boolean fileExists = fs.existsBlocking(toFileFullPath);
      if (fileExists) {
        log.warn("[File] File already exists: {}", toFileFullPath);
      }

      Future<Void> f_moveOrCopy = null;
      switch (mode) {
        case "COPY":
          f_moveOrCopy = fileExists
              ? Future.succeededFuture()
              : fs.copy(fromFileFullPath, toFileFullPath);
          break;
        case "MOVE":
          f_moveOrCopy = fileExists
              ? Future.succeededFuture()
              : fs.move(fromFileFullPath, toFileFullPath);
          break;
        case "INFO":
          f_moveOrCopy = Future.succeededFuture();
          break;
        default:
          log.error("[File] Unknown mode: {}", mode);
          return Future.failedFuture("Unknown mode: " + mode);
      }

      return f_moveOrCopy
          .onFailure(err -> {
            log.error("[File] Failed to {} file {} to {}: {}",
                mode,
                fromFileFullPath,
                toFileFullPath,
                err.getMessage());
          })
          .onSuccess(ar2 -> {
            log.trace("[File] Success to {} file {} to {}",
                mode,
                fromFileFullPath,
                toFileFullPath);
          })
          .compose(ar2 -> {
            return addDoc(docs);
          });

    } catch (Exception e) {
      log.error("Error: {}", e.getMessage());
      return Future.failedFuture(e);
    }
  }

  /**
   * Move files from one directory to another, ignore hidden files
   * <p>
   *
   * @param fs              the file system
   * @param fromFolder      the source directory, absolute path
   * @param toFolder        the destination directory, absolute path
   * @param toSubFolderDeep the depth of sub-folders to create
   * @param toSubFolderLen  the length of each sub-folder
   * @param mode            the mode to use, COPY or MOVE, INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public static Future<Void> setFolderInfo(
      FileSystem fs,
      String fromFolder,
      String toFolder,
      int toSubFolderDeep,
      int toSubFolderLen,
      String mode) {

    if (!fs.existsBlocking(fromFolder) || !fs.propsBlocking(fromFolder).isDirectory()) {
      log.error("[Folders] Source path is not a directory: {}", fromFolder);
      return Future.failedFuture("Source path is not a directory: " + fromFolder);
    }

    if (!fs.existsBlocking(toFolder) || !fs.propsBlocking(toFolder).isDirectory()) {
      log.error("[Folders] Destination path is not a directory: {}", toFolder);
      return Future.failedFuture("Destination path is not a directory: " + toFolder);
    }

    List<String> fileList = fs.readDirBlocking(fromFolder);

    try {
      for (int i = 0; i < fileList.size(); i++) {
        String fileInFolder = fileList.get(i);
        FileProps inProps = fs.propsBlocking(fileInFolder);

        if (inProps.isDirectory()) {
          log.info("[Folders] {}", fileInFolder);
          setFolderInfo(fs, fileInFolder, toFolder, toSubFolderDeep, toSubFolderLen, mode).onSuccess(rst -> {
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

          log.info("[File] {}", fileInFolder);
          setFileInfo(fs, fileInFolder, fileName, toFolder, toSubFolderDeep, toSubFolderLen, mode)
              .onFailure(err -> {
                throw new RuntimeException(
                    "RuntimeException: Failed to " + mode + " file " + fileInFolder + ": " + err.getMessage());
              });
        } else {
          // do nothing
        }
      }
      return Future.succeededFuture();

    } catch (Exception e) {
      log.error("[Folders] Error: {}", e.getMessage());
      return Future.failedFuture(e);
    }

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

}
