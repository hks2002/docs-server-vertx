/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-23 18:32:12                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


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
import java.util.List;

import org.bouncycastle.util.encoders.Hex;

import com.da.docs.VertxHolder;
import com.da.docs.service.DocsService;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FSUtils {
  private static JsonObject config = VertxHolder.appConfig;
  private static JsonObject docsConfig = Utils.isWindows()
      ? config.getJsonObject("docs").getJsonObject("windows")
      : config.getJsonObject("docs").getJsonObject("linux");
  private static JsonObject uploadConfig = config.getJsonObject("upload", new JsonObject());
  private static String docsRoot = docsConfig.getString("docsRoot", Utils.isWindows() ? "c:/docs" : "/mnt/docs");
  private static int folderDeep = uploadConfig.getInteger("folderDeep", 0);;
  private static int folderLen = uploadConfig.getInteger("folderLen", 3);

  private static String computerMd5(String fileFullPath) {
    Buffer buf = VertxHolder.fs.readFileBlocking(fileFullPath);
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] bytes = buf.getBytes();
      md.update(bytes);
      byte[] digest = md.digest();

      return new String(Hex.encode(digest));
    } catch (Exception e) {
      log.error("Failed to compute MD5 for file {}: {}", fileFullPath, e.getMessage());
      return "";
    }
  }

  public static void updateFileModifiedDate(String fileFullPath, long timestamp) {
    try {
      Path path = Path.of(fileFullPath);
      FileTime fileTime = FileTime.from(Instant.ofEpochMilli(timestamp));
      Files.setLastModifiedTime(path, fileTime);
      log.info("Successfully updated the modified date for file: {}", fileFullPath);
    } catch (Exception e) {
      log.error("Failed to update the modified date for file: {}", fileFullPath, e);
    }
  }

  public static void updateFileModifiedDate(String fileFullPath, String timestamp) {
    try {
      updateFileModifiedDate(fileFullPath, Long.parseLong(timestamp));
    } catch (Exception e) {
      log.error("Failed parse long from String", timestamp);
    }
  }

  public static boolean isFileExists(String toFileName) {
    String toFolder = FSUtils.docsRoot;
    // get the destination folder
    String toSubFolder = CommonUtils.getPathByFileName(toFileName, FSUtils.folderDeep, FSUtils.folderLen);
    String toFolderFullPath = toFolder + '/' + toSubFolder;
    String toFileFullPath = toFolderFullPath + '/' + toFileName;

    return VertxHolder.fs.existsBlocking(toFileFullPath);
  }

  /**
   * Move file to target folder
   * <p>
   *
   * @param fromFileFullPath the source file, absolute path
   * @param toFileName       the destination file name
   * @param fileId           the file id
   * @param mode             the mode, MOVE or COPY or INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public static Future<Object> setFileInfo(
      String fromFileFullPath,
      String toFileName,
      String fileId,
      String mode) {

    if (!VertxHolder.fs.existsBlocking(fromFileFullPath)
        || VertxHolder.fs.propsBlocking(fromFileFullPath).isDirectory()) {
      log.error("[File] Source file is not a file: {}", fromFileFullPath);
      return Future.failedFuture("");
    }
    String toFolder = FSUtils.docsRoot;
    if (!VertxHolder.fs.existsBlocking(toFolder) || !VertxHolder.fs.propsBlocking(toFolder).isDirectory()) {
      log.error("[File] Destination path is not a directory: {}", toFolder);
      return Future.failedFuture("");
    }

    try {
      FileProps inProps = VertxHolder.fs.propsBlocking(fromFileFullPath);
      // get the destination folder
      String toSubFolder = CommonUtils.getPathByFileName(toFileName, FSUtils.folderDeep, FSUtils.folderLen);
      String toFolderFullPath = toFolder + '/' + toSubFolder;
      String toLocation = toSubFolder + '/' + toFileName;
      String md5 = FSUtils.computerMd5(fromFileFullPath);

      LocalDateTime createAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(inProps.creationTime()),
          ZoneId.systemDefault());
      LocalDateTime modifiedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(inProps.lastModifiedTime()),
          ZoneId.systemDefault());

      // make sure toFolder exists
      if (!VertxHolder.fs.existsBlocking(toFolderFullPath)) {
        log.trace("[File] Make dir: {}", toFolderFullPath);
        VertxHolder.fs.mkdirsBlocking(toFolderFullPath);
      }
      String toFileFullPath = toFolderFullPath + '/' + toFileName;
      boolean fileExists = VertxHolder.fs.existsBlocking(toFileFullPath);
      if (fileExists) {
        log.warn("[File] File already exists: {}", toFileFullPath);
      }

      Future<Void> f_moveOrCopy = null;
      switch (mode) {
        case "COPY":
          f_moveOrCopy = fileExists
              ? Future.succeededFuture()
              : VertxHolder.fs.copy(fromFileFullPath, toFileFullPath);
          break;
        case "MOVE":
          f_moveOrCopy = fileExists
              ? Future.succeededFuture()
              : VertxHolder.fs.move(fromFileFullPath, toFileFullPath);
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
            DocsService docsService = new DocsService();
            return docsService.addDocs(toFileName, inProps.size(), toLocation, fileId, md5,
                createAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                modifiedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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
   * @param fromFolder the source directory, absolute path
   * @param mode       the mode to use, COPY or MOVE, INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public static Future<Void> setFolderInfo(
      String fromFolder,
      String mode) {

    if (!VertxHolder.fs.existsBlocking(fromFolder) || !VertxHolder.fs.propsBlocking(fromFolder).isDirectory()) {
      log.error("[Folders] Source path is not a directory: {}", fromFolder);
      return Future.failedFuture("Source path is not a directory: " + fromFolder);
    }

    String toFolder = FSUtils.docsRoot;
    if (!VertxHolder.fs.existsBlocking(toFolder) || !VertxHolder.fs.propsBlocking(toFolder).isDirectory()) {
      log.error("[Folders] Destination path is not a directory: {}", toFolder);
      return Future.failedFuture("Destination path is not a directory: " + toFolder);
    }

    List<String> fileList = VertxHolder.fs.readDirBlocking(fromFolder);

    try {
      for (int i = 0; i < fileList.size(); i++) {
        String fileInFolder = fileList.get(i);
        FileProps inProps = VertxHolder.fs.propsBlocking(fileInFolder);

        if (inProps.isDirectory()) {
          log.info("[Folders] {}", fileInFolder);
          setFolderInfo(fileInFolder, mode).onSuccess(rst -> {
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
          setFileInfo(fileInFolder, fileName, "", mode)
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

}
