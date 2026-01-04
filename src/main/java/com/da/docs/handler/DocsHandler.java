/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 17:04:15                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.da.docs.VertxApp;
import com.da.docs.annotation.GetMapping;
import com.da.docs.service.LogService;
import com.da.docs.utils.CommonUtils;
import com.da.docs.utils.ITextTools;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.MimeMapping;
import io.vertx.core.impl.Utils;
import io.vertx.core.internal.net.RFC3986;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorizations;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@GetMapping("/docs-api/docs/*")
public class DocsHandler implements Handler<RoutingContext> {

  private boolean includeHidden = true;
  private String defaultContentEncoding = Charset.defaultCharset().name();

  private String docsRoot = "";
  private boolean waterMarkEnable = false;
  private boolean showCompany = false;
  private boolean showBP = false;
  private boolean showUser = false;
  private String companyName = "";
  private boolean saveWithDocName = false;

  private Set<String> waterMakerFileTypes = new HashSet<>();
  private Set<String> waterMakerExcludeNames = new HashSet<>();

  public DocsHandler() {
    JsonObject appConfig = VertxApp.appConfig;
    JsonObject docsConfig = appConfig.getJsonObject("docs");
    JsonObject waterMarkConfig = docsConfig.getJsonObject("waterMark");

    this.docsRoot = Utils.isWindows()
        ? docsConfig.getJsonObject("docsRoot").getString("windows")
        : docsConfig.getJsonObject("docsRoot").getString("linux");

    this.waterMarkEnable = waterMarkConfig.getBoolean("enable", waterMarkEnable);
    this.showCompany = waterMarkConfig.getBoolean("showCompany", showCompany);
    this.showBP = waterMarkConfig.getBoolean("showBP", showBP);
    this.showUser = waterMarkConfig.getBoolean("showUser", showUser);
    this.companyName = waterMarkConfig.getString("companyName", companyName);
    JsonArray fileTypes = waterMarkConfig.getJsonArray("fileTypes", new JsonArray());
    JsonArray excludeNames = waterMarkConfig.getJsonArray("excludeNames", new JsonArray());

    for (int i = 0; i < fileTypes.size(); i++) {
      String fileType = fileTypes.getString(i);
      waterMakerFileTypes.add(fileType.toLowerCase());
    }
    for (int i = 0; i < excludeNames.size(); i++) {
      String excludeName = excludeNames.getString(i);
      waterMakerExcludeNames.add(excludeName.toUpperCase());
    }

    this.saveWithDocName = appConfig.getJsonObject("docs").getBoolean("saveWithDocName", true);
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

    // decode URL path
    String uriDecodedPath = RFC3986.decodeURIComponent(context.normalizedPath(), true);
    // if the normalized path is null it cannot be resolved
    if (uriDecodedPath == null) {
      Response.badRequest(context, "Invalid path: " + context.request().path());
      return;
    }
    // will normalize and handle all paths as UNIX paths
    String path = RFC3986.removeDotSegments(uriDecodedPath.replace('\\', '/'));
    sendStatic(context, path);
  }

  private void sendStatic(RoutingContext context, String requestPath) {
    final String localFilePath = getLocalFile(requestPath);

    // skip hidden files
    int idx = localFilePath.lastIndexOf('/');
    String name = localFilePath.substring(idx + 1);
    if (name.length() > 0 && name.charAt(0) == '.') {
      Response.badRequest(context);
      return;
    }

    // verify if the file exists
    VertxApp.fs.exists(localFilePath)
        .onFailure(err -> {
          log.error("Failed to check file exists: {}, {}", localFilePath, err.getCause());
          Response.internalError(context, "Failed to check file exists");
        })
        .onSuccess(exists -> {
          if (!exists) {
            Response.notFound(context);
            return;
          }

          VertxApp.fs.props(localFilePath)
              .onFailure(err -> {
                log.error("Failed to get file props: {}, {}", localFilePath, err.getCause());
                Response.internalError(context, "Failed to get file props");
              })
              .onSuccess(fProps -> {
                if (fProps.isDirectory()) {
                  sendDirectory(context, requestPath, localFilePath);
                } else {
                  sendFile(context, localFilePath);
                }
              });
        });
  }

  /**
   * sibling means that we are being upgraded from a directory to a index
   */
  private void sendDirectory(RoutingContext context, String requestPath, String localFilePath) {
    // in order to keep caches in a valid state we need to assert that
    // the user is requesting a directory (ends with /)
    if (!requestPath.endsWith("/")) {
      context.response()
          .putHeader(HttpHeaders.LOCATION, requestPath + "/")
          .setStatusCode(301)
          .end();
      return;
    }
    HttpServerResponse response = context.response();

    VertxApp.fs.readDir(localFilePath)
        .onFailure(err -> {
          log.error("Failed to read directory: {}, {}", localFilePath, err.getCause());
          Response.internalError(context, "Failed to read directory");
        }).onSuccess(list -> {
          // log.info("{}, {}", requestPath, list);

          String accept = CommonUtils.getAccept(context);

          switch (accept) {
            case "application/json":
              JsonArray json = new JsonArray();

              for (String s : list) {
                String fileName = s.substring(s.lastIndexOf(File.separatorChar) + 1);
                // skip dot files
                if (!includeHidden && fileName.charAt(0) == '.') {
                  continue;
                }
                FileProps fProps = VertxApp.fs.propsBlocking(s);
                if (fProps == null) {
                  continue;
                }
                JsonObject o = new JsonObject();
                o.put("name", fileName);
                o.put("size", fProps.size());
                o.put("lastModified", fProps.lastModifiedTime());
                o.put("url", requestPath + fileName + (fProps.isDirectory() ? "/" : ""));
                o.put("isDirectory", fProps.isDirectory());
                json.add(o);
              }

              response.putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8").end(json.encode());
              break;
            default:
              Buffer b = Buffer.buffer();

              for (String s : list) {
                String fileName = s.substring(s.lastIndexOf(File.separatorChar) + 1);
                // skip dot files
                if (!includeHidden && fileName.charAt(0) == '.') {
                  continue;
                }
                b.appendString(fileName);
                b.appendInt('\n');
              }
              response.putHeader(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8").end(b);
          }
        });
  }

  private void sendFile(RoutingContext context, String localFilePath) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    String ip = CommonUtils.getTrueRemoteIp(request);
    String fileName = getFileName(localFilePath);

    // here user should not be null, it has been checked before
    User user = Optional.ofNullable(context.user()).orElse(User.create(new JsonObject()));
    String loginName = user.principal().getString("login_name", "");
    String fullName = user.principal().getString("full_name", "");

    // bp info
    Session session = context.session();
    final String bpCode = Optional.ofNullable((String) session.get("BP_CODE")).orElse("");
    final String bpName = Optional.ofNullable((String) session.get("BP_NAME")).orElse("");

    // log it
    LogService.addLog("DOC_ACCESS_SUCCESS", ip, loginName, fullName, fileName, bpCode, bpName);

    final String wmText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
        (showCompany ? ' ' + companyName : "") +
        (showUser ? ' ' + fullName : "") +
        (showBP ? ' ' + bpName : "");

    // guess content type
    String extension = getFileExtension(localFilePath);
    final String outFileName = encodeFileName(
        wmText +
            (saveWithDocName
                ? ' ' + fileName
                : ' ' + CommonUtils.addRadomChar(getFileNameWithoutExtension(fileName)) + " DO NOT SAVE ME"))
        + "." + extension;

    writeDateHeader(response);

    // add water mark for some file types
    if (waterMarkEnable &&
        extension != null &&
        waterMakerFileTypes.contains(extension) &&
        !CommonUtils.nameMatch(fileName, waterMakerExcludeNames)) {

      VertxApp.fs.createTempFile(null, null)
          .onSuccess(tempFile -> {
            try {
              FileInputStream fis = new FileInputStream(localFilePath);
              FileOutputStream fos = new FileOutputStream(tempFile);

              try (fis; fos) {// auto close
                boolean b = ITextTools.addWatermark(extension, fis, fos, wmText, 30, bpCode.isEmpty() ? 0.3f : 0.03f);
                if (b) {// add watermark success
                  log.info("Add watermark to file: {}", localFilePath);
                  writeDispositionHeaders(response, "pdf", outFileName + (extension.equals("pdf") ? "" : ".pdf"));

                  response.sendFile(tempFile)
                      .onComplete(ar -> {
                        VertxApp.fs.delete(tempFile);
                      });
                } else { // failed, send original file
                  log.error("Failed to add watermark to file: {}", localFilePath);
                  writeDispositionHeaders(response, extension, outFileName);

                  response.sendFile(localFilePath)
                      .onComplete(ar -> {
                        VertxApp.fs.delete(tempFile);
                      });
                }

              }
            } catch (Exception e) {
              log.error("Failed to add watermark: {}", e.getCause());
              response.setStatusCode(500).end("Error adding watermark");
            }
          });

    } else { // send original file
      writeDispositionHeaders(response, extension, outFileName);
      response.sendFile(localFilePath);
    }
  }

  /**
   * Write the date header.
   *
   * @param request
   */
  private void writeDateHeader(HttpServerResponse response) {
    MultiMap headers = response.headers();
    // date header is mandatory
    headers.set("date", io.vertx.ext.web.impl.Utils.formatRFC1123DateTime(System.currentTimeMillis()));
  }

  /**
   * Write the content type and content disposition headers.
   *
   * @param response
   * @param extension
   * @param outFileName
   */
  private void writeDispositionHeaders(HttpServerResponse response, String extension, String outFileName) {
    String contentType = Optional.ofNullable(MimeMapping.mimeTypeForExtension(extension))
        .orElse("application/octet-stream");

    if (contentType.startsWith("text")) {
      response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + defaultContentEncoding);
    } else {
      response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }
    response.putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + outFileName + "\"");
  }

  /**
   * Get the local file path from the request path.
   *
   * @param requestPath
   * @return
   */
  private String getLocalFile(String requestPath) {
    String localFilePath = docsRoot + requestPath.replace("/docs-api/docs", "");
    log.trace("File to serve is " + localFilePath);
    return localFilePath;
  }

  /**
   * Extracts and returns the file extension from a given filename.
   *
   * @param file The filename or path to extract extension from
   * @return The lowercase file extension without the dot, or null if no valid
   *         extension exists
   */
  private String getFileExtension(String file) {
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
  private String getFileNameWithoutExtension(String file) {
    if (file == null)
      return null;
    int dotIndex = file.lastIndexOf(46);
    if (dotIndex == -1)
      return file;
    return file.substring(0, dotIndex);
  }

  /**
   * Get the file name from the path.
   *
   * @param filePath
   * @return
   */
  private String getFileName(String filePath) {
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
   * Encode the file name to be used in the Content-Disposition header.
   *
   * @param fileName
   * @return
   */
  private static String encodeFileName(String fileName) {
    try {
      String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
      return encodedFileName.replace("+", " ");
    } catch (Exception e) {
      return fileName;
    }
  }

}
