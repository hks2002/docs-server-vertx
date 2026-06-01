/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-23 22:20:19                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.serviceStatic;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Set;

import org.bouncycastle.util.encoders.Hex;

import com.da.docs.service.DocsService;
import com.da.docs.utils.CommonUtils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FS {
  private static Vertx vertx = null;
  public static FileSystem fs = null;

  private static final Set<Long> INVALID_FILE_SIZES = Set.of(562L, 581L);
  private static String docsRoot = "C:/docs";
  private static Integer folderDeep = 2;
  private static Integer folderLen = 3;

  /**
   * Static class, must be initialized, be careful the null value
   * 
   * @param vertx
   */
  public static void setup(Vertx vertx) {
    FS.vertx = vertx;
    FS.fs = vertx.fileSystem();
    JsonObject appConfig = vertx.getOrCreateContext().config();

    JsonObject docsConfig = appConfig.getJsonObject("docs");
    FS.docsRoot = Utils.isWindows()
        ? docsConfig.getJsonObject("docsRoot").getString("windows", "c:/docs")
        : docsConfig.getJsonObject("docsRoot").getString("linux", "/mnt/docs");

    fs.props(docsRoot)
        .onSuccess(props -> {
          if (!props.isDirectory()) {
            throw new IllegalArgumentException("Docs root is not a directory: " + docsRoot);
          }
        })
        .onFailure(e -> {
          throw new IllegalArgumentException("Docs root is not a directory: " + docsRoot);
        });

    JsonObject uploadConfig = appConfig.getJsonObject("upload",
        JsonObject.of("folderDeep", 2, "folderLen", 3));
    FS.folderDeep = uploadConfig.getInteger("folderDeep", 2);
    FS.folderLen = uploadConfig.getInteger("folderLen", 3);
  }

  public static String getDocsRoot() {
    return docsRoot;
  }

  /**
   * Get the local file path from the request path.
   *
   * @param requestPath
   * @return
   */
  public static String getLocalFile(String requestPath) {
    String localFilePath = docsRoot + requestPath.replace("/docs-api/docs", "");
    log.trace("File to serve is " + localFilePath);
    return localFilePath;
  }

  /**
   * Get the file name from the path.
   *
   * @param filePath
   * @return
   */
  public static String getFileName(String filePath) {
    int lastSeparatorIndex = filePath.lastIndexOf('/');
    if (lastSeparatorIndex == -1) {
      lastSeparatorIndex = filePath.lastIndexOf('\\');
    }

    if (lastSeparatorIndex != -1 && lastSeparatorIndex < filePath.length() - 1) {
      return filePath.substring(lastSeparatorIndex + 1);
    } else {
      return filePath;
    }
  }

  /**
   * Extracts and returns the file extension from a given filename.
   *
   * @param file The filename or path to extract extension from
   * @return The lowercase file extension without the dot, or null if no valid
   *         extension exists
   */
  public static String getFileExtension(String file) {
    int li = file.lastIndexOf(46);
    if (li != -1 && li != file.length() - 1) {
      return file.substring(li + 1).toLowerCase();
    } else {
      return null;
    }
  }

  /**
   * Get the file name without extension.
   *
   * @param file
   * @return
   */
  public static String getFileNameWithoutExtension(String file) {
    int dotIndex = file.lastIndexOf(46);
    if (dotIndex == -1)
      return file;
    return file.substring(0, dotIndex);
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

  /**
   * Get the last modified time of a file
   * 
   * @param fileFullPath
   * @return
   */
  public static Future<Long> getFileModifiedTime(String fileFullPath) {
    return fs.props(fileFullPath).compose(props -> Future.succeededFuture(props.lastModifiedTime()));
  }

  /**
   * Encode the file name to be used in the Content-Disposition header.
   *
   * @param fileName
   * @return
   */
  public static String encodeFileName(String fileName) {
    try {
      String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
      return encodedFileName.replace("+", " ");
    } catch (Exception e) {
      return fileName;
    }
  }

  /**
   * Compute MD5 checksum for a file
   */
  public static Future<String> computerMd5(String fileFullPath) {
    return vertx.executeBlocking(() -> {
      Buffer buf = fs.readFileBlocking(fileFullPath);
      try {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = buf.getBytes();
        md.update(bytes);
        byte[] digest = md.digest();

        return new String(Hex.encode(digest));
      } catch (Exception e) {
        log.error("Failed to compute MD5 for file {}: {}", fileFullPath, e.getCause());
        return new String("Failed");
      }
    });
  }

  /**
   * Check if file exists, and if file size is
   * <b>INVALID_FILE_SIZES 581</b> bytes, will delete
   * it and treat as not exists
   */
  public static Future<Boolean> isFileExists(String fileFullPath) {
    return fs.props(fileFullPath)
        .compose(props -> {
          if (!INVALID_FILE_SIZES.contains(props.size())) {
            log.debug("File exists: {}", fileFullPath);
            return Future.succeededFuture(true);
          }

          log.warn("File {} has invalid size ({}). Deleting.", fileFullPath, props.size());
          return fs.delete(fileFullPath).compose(v -> Future.succeededFuture(false));
        })
        .recover(throwable -> {
          log.debug("File does not exist or cannot be accessed: {}", fileFullPath);
          return Future.succeededFuture(false);
        });
  }

  /**
   * Check if folder exists, if not, create it
   */
  public static Future<Boolean> isFoldExists(String foldFullPath) {
    return fs.exists(foldFullPath)
        .compose(toFolderExists -> {
          // make sure the target folder exists
          if (!toFolderExists) {
            log.trace("[File][Move] Make dir: {}", foldFullPath);
            return fs.mkdirs(foldFullPath).compose(v -> Future.succeededFuture(true));
          }
          return Future.succeededFuture(true);
        });
  }

  /**
   * Safely create a link from oriFilePath to linkFilePath.
   * <p>
   * Before creating the link, the existing file at linkFilePath is backed up.
   * If the link creation succeeds, the backup is deleted.
   * If the link creation fails, the backup is restored.
   * </p>
   *
   * @param linkFilePath the path where the link will be created
   * @param oriFilePath  the path to the original file
   * @return Future that completes when the link is created or fails with an error
   */
  public static Future<Void> safeLink(String linkFilePath, String oriFilePath) {
    String backupPath = linkFilePath + ".bak";
    return fs.move(linkFilePath, backupPath)
        .compose(v -> fs.link(linkFilePath, oriFilePath))
        .compose(v -> fs.delete(backupPath))
        .recover(throwable -> {
          log.error("Failed to create link, restoring backup: {}", backupPath);
          return fs.move(backupPath, linkFilePath)
              .compose(v -> Future.failedFuture(throwable));
        });
  }

  /**
   * Update file modified date
   */
  public static Future<Boolean> updateFileModifiedDate(String fileFullPath, long timestamp) {
    return vertx.executeBlocking(() -> {
      try {
        Path path = Path.of(fileFullPath);
        FileTime fileTime = FileTime.from(Instant.ofEpochMilli(timestamp));
        Files.setLastModifiedTime(path, fileTime);
        return true;
      } catch (Exception e) {
        return false;
      }
    });
  }

  /**
   * Update file modified date
   */
  public static Future<Boolean> updateFileModifiedDate(String fileFullPath, String timestamp) {
    try {
      return updateFileModifiedDate(fileFullPath, Long.parseLong(timestamp));
    } catch (Exception e) {
      log.error("Failed parse long from String", timestamp);
      return Future.succeededFuture(false);
    }
  }

  /**
   * Move file to target folder
   * <p>
   *
   * @param fromFileFullPath the source file, absolute path
   * @param fileName         the final file name, with extension
   * @param mode             the mode, MOVE or COPY or INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public static Future<Void> moveFile(String fromFileFullPath, String fileName, String mode) {
    String toFolderFullPath = docsRoot + '/' + getFolderPathByFileName(fileName);
    String toFileFullPath = toFolderFullPath + '/' + fileName;

    Future<Boolean> f1 = isFileExists(fromFileFullPath);
    Future<Boolean> f2 = isFileExists(toFileFullPath);
    Future<Boolean> f3 = isFoldExists(toFolderFullPath);

    return Future.all(f1, f2, f3).compose(data -> {
      Boolean sourceFileExists = data.resultAt(0);
      Boolean targetFileExists = data.resultAt(1);

      if (!sourceFileExists) {
        log.error("[File][Move] Source file is not a file: {}", fromFileFullPath);
        return Future.failedFuture("Source file is not a file: " + fromFileFullPath);
      }

      Future<Void> f_moveOrCopy = null;
      switch (mode) {
        case "COPY":
          if (targetFileExists) {
            log.warn("[File][Copy] File already exists: {}", toFileFullPath);
            return Future.succeededFuture();
          }
          f_moveOrCopy = fs.copy(fromFileFullPath, toFileFullPath);
          break;
        case "MOVE":
          if (targetFileExists) {
            log.warn("[File][Move] File already exists: {}", toFileFullPath);
            return Future.succeededFuture();
          }
          f_moveOrCopy = fs.move(fromFileFullPath, toFileFullPath);
          break;
        case "UPDATE":
          f_moveOrCopy = targetFileExists
              ? fs.delete(toFileFullPath)
                  .compose(ar -> fs.move(fromFileFullPath, toFileFullPath))
              : fs.move(fromFileFullPath, toFileFullPath);
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
                err.getCause());
          })
          .onSuccess(ar2 -> {
            log.trace("[File] Success {} file {} to {}",
                mode,
                fromFileFullPath,
                toFileFullPath);
          });
    });
  }

  /**
   * Move files from one directory to another, ignore hidden files
   * <p>
   *
   * @param fromFolder the source directory, absolute path
   * @param mode       the mode to use, COPY or MOVE, UPDATE, INFO
   *
   * @Note Top level is always ne character[0-9A-Z]
   *       and remove "TDS", "OMSD", "GIM" ... from file name
   * @Note this method does not update database
   */
  public static Future<Void> buildFolderInfo(String fromFolder, String mode) {
    FileProps foldProps = fs.propsBlocking(fromFolder);
    if (!foldProps.isDirectory()) {
      log.error("[Folders] Source path is not a directory: {}", fromFolder);
      return Future.failedFuture("Source path is not a directory: " + fromFolder);
    }

    return fs.readDir(fromFolder).compose(fileList -> {
      try {
        for (int i = 0; i < fileList.size(); i++) {
          String fileInFolder = fileList.get(i);
          FileProps inProps = fs.propsBlocking(fileInFolder);

          if (inProps.isDirectory()) {
            log.info("[Folders][INFO_BUILD] {}", fileInFolder);
            buildFolderInfo(fileInFolder, mode)
                .onSuccess(rst -> {
                  // delete the empty folder
                  // fs.delete(fileInFolder);
                });

          } else if (inProps.isRegularFile()) {
            String fileName = getFileName(fileInFolder);

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
              case "UPDATE":
                moveFile(fileInFolder, fileName, mode)
                    .onFailure(err -> {
                      log.error("[File] Failed to {} file {} to {}: {}", mode, fileInFolder, fileName,
                          err.getCause());
                    })
                    .onSuccess(ar2 -> {
                      new DocsService().addOrModifyFileInfo(fileName, "");
                    });
                break;
              case "INFO":
                new DocsService().addOrModifyFileInfo(fileName, "");
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
        log.error("[Folders] Error: {}", e.getCause());
        return Future.failedFuture(e);
      }
    });

  }

}