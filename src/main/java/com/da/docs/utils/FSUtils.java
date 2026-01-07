/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 19:18:10                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;

import org.bouncycastle.util.encoders.Hex;

import com.da.docs.VertxApp;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FSUtils {
  // Static class, must be initialized, be careful the null value
  private static JsonObject docsConfig = VertxApp.appConfig == null
      ? JsonObject.of("docsRoot", JsonObject.of("windows", "C:/docs", "linux", "/home/docs"))
      : VertxApp.appConfig.getJsonObject("docs");
  private static String docsRoot = Utils.isWindows()
      ? docsConfig.getJsonObject("docsRoot").getString("windows")
      : docsConfig.getJsonObject("docsRoot").getString("linux");

  private static JsonObject uploadConfig = VertxApp.appConfig == null ? JsonObject.of("folderDeep", 0, "folderLen", 3)
      : VertxApp.appConfig.getJsonObject("upload");
  private static Integer folderDeep = uploadConfig.getInteger("folderDeep", 0);
  private static Integer folderLen = uploadConfig.getInteger("folderLen", 3);
  private static FileSystem fs = VertxApp.fs;

  public static void setup(String docRoot, int folderDeep, int folderLen, FileSystem fileSystem) {
    FSUtils.docsRoot = docRoot;
    FSUtils.folderDeep = folderDeep;
    FSUtils.folderLen = folderLen;
    FSUtils.fs = fileSystem;
  }

  public static String getDocsRoot() {
    if (!fs.existsBlocking(docsRoot) || !fs.propsBlocking(docsRoot).isDirectory()) {
      log.error("[Folders] Destination path is not a directory: {}", docsRoot);
      throw new RuntimeException("Destination path is not a directory: " + docsRoot);
    }
    return docsRoot;
  }

  public static int getFolderDeep() {
    return folderDeep;
  }

  public static int getFolderLen() {
    return folderLen;
  }

  /**
   * Compute MD5 checksum for a file
   */
  public static String computerMd5(String fileFullPath) {
    Buffer buf = fs.readFileBlocking(fileFullPath);
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

  /**
   * Update file modified date
   */
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

  /**
   * Update file modified date
   */
  public static void updateFileModifiedDate(String fileFullPath, String timestamp) {
    try {
      updateFileModifiedDate(fileFullPath, Long.parseLong(timestamp));
    } catch (Exception e) {
      log.error("Failed parse long from String", timestamp);
    }
  }

  /**
   * Get the last modified time of a file
   * 
   * @param fileFullPath
   * @return
   */
  public static Long getFileModifiedTime(String fileFullPath) {
    try {
      Path path = Path.of(fileFullPath);
      FileTime fileTime = Files.getLastModifiedTime(path);
      return fileTime.toInstant().toEpochMilli();
    } catch (Exception e) {
      log.error("Failed to update the modified date for file: {}", fileFullPath, e);
      return 0L;
    }
  }

  /**
   * Check if file exists, and if file size is 581 bytes, delete it and return
   * false
   */
  public static boolean isFileExists(String toFileFullPath) {
    boolean b = fs.existsBlocking(toFileFullPath);
    if (!b) {
      return false;
    }
    Buffer buf = fs.readFileBlocking(toFileFullPath);
    if (buf.length() == 581) {
      fs.deleteBlocking(toFileFullPath);
      return false;
    } else {
      return true;
    }
  }

  /**
   * Generates a file path based on the given file name, based on folder depth,and
   * folder length.
   *
   * @param fileName the file name
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @return the generated file path
   */
  public static String getFolderPathByFileName(String fileName) {
    return getFolderPathByFileName(fileName, folderDeep, folderLen);
  }

  /**
   * Generates a file path based on the given file name, sub-folder depth, and
   * sub-folder length.
   *
   * @param fileName        the file name
   * @param toSubFolderDeep the depth of sub-folders to create, if -1, no
   *                        sub-folder
   * @param toSubFolderLen  the length of each sub-folder
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @return the generated file path
   */
  public static String getFolderPathByFileName(
      String fileName,
      int toSubFolderDeep,
      int toSubFolderLen) {
    if (toSubFolderDeep <= 0) {
      return "";
    }

    int dotIndex = fileName.lastIndexOf(".");
    String fileNameNoExt = dotIndex > 0
        ? fileName.substring(0, dotIndex)
        : fileName;

    // remove "TDS", "OMSD", "GIM" ... from file name, ignore case
    // keep only [A-Za-z0-9] in file name
    String cleanName = fileNameNoExt
        .replaceAll("(?i)TDS", "")
        .replaceAll("(?i)OMSD", "")
        .replaceAll("(?i)DWG", "")
        .replaceAll("(?i)REV", "")
        .replaceAll("(?i)GIM", "")
        .replaceAll("(?i)NOTICE", "")
        .replaceAll("(?i)TECHNIQUE", "")
        .replaceAll("(?i)D'UTILISATIONS", "")
        .replaceAll("(?i)D'UTILISATION", "")
        .replaceAll("(?i)D'INSTRUCTIONS", "")
        .replaceAll("(?i)D'INSTRUCTION", "")
        .replaceAll("(?i)INSTRUCTIONS", "")
        .replaceAll("(?i)INSTRUCTION", "")
        .replaceAll("(?i)INFORMATION", "")
        .replaceAll("(?i)USER", "")
        .replaceAll("(?i)GUIDE", "")
        .replaceAll("(?i)MANUAL", "")
        .replaceAll("(?i)MANUEL", "")
        .replaceAll("[^A-Za-z0-9]", "")
        .toUpperCase();

    // get left toSubFolderDeep * toSubFolderLen chars, if less than it, add 0
    String subFolders = CommonUtils.withRightPad(cleanName, toSubFolderDeep * toSubFolderLen, '0');
    // top level fixed to 0-9 and A-Z
    StringBuilder sb = new StringBuilder(subFolders.substring(0, 1));
    for (int i = 0; i < toSubFolderDeep; i++) {
      String subFolderName = subFolders.substring(
          i * toSubFolderLen,
          (i + 1) * toSubFolderLen);
      // These names are reserved for Windows
      if (subFolderName.equals("CON") ||
          subFolderName.equals("PRN") ||
          subFolderName.equals("AUX") ||
          subFolderName.equals("NUL")) {
        subFolderName = "000";
      }
      sb.append('/').append(subFolderName);
    }

    return sb.toString();
  }
}
