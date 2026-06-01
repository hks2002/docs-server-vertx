/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-24 16:20:55                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.serviceStatic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.da.docs.service.LogService;
import com.da.docs.utils.CommonUtils;
import com.da.docs.utils.ITextTools;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.MimeMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RESPONSE {
  private static String defaultContentEncoding = Charset.defaultCharset().name();

  private static String companyName = "";
  private static boolean includeHidden = true;
  private static boolean waterMarkEnable = false;
  private static boolean showCompany = false;
  private static boolean showBP = false;
  private static boolean showUser = false;
  private static boolean saveWithDocName = false;

  private static Set<String> waterMakerFileTypes = new HashSet<>();
  private static Set<String> waterMakerExcludeNames = new HashSet<>();

  /**
   * Static class, must be initialized, be careful the null value
   * 
   * @param vertx
   */
  public static void setup(Vertx vertx) {
    JsonObject appConfig = vertx.getOrCreateContext().config();
    JsonObject docsConfig = appConfig.getJsonObject("docs");
    JsonObject waterMarkConfig = docsConfig.getJsonObject("waterMark");

    RESPONSE.waterMarkEnable = waterMarkConfig.getBoolean("enable");
    RESPONSE.showCompany = waterMarkConfig.getBoolean("showCompany");
    RESPONSE.showBP = waterMarkConfig.getBoolean("showBP");
    RESPONSE.showUser = waterMarkConfig.getBoolean("showUser");
    RESPONSE.companyName = waterMarkConfig.getString("companyName");
    JsonArray fileTypes = waterMarkConfig.getJsonArray("fileTypes");
    JsonArray excludeNames = waterMarkConfig.getJsonArray("excludeNames");

    for (int i = 0; i < fileTypes.size(); i++) {
      String fileType = fileTypes.getString(i);
      RESPONSE.waterMakerFileTypes.add(fileType.toLowerCase());
    }
    for (int i = 0; i < excludeNames.size(); i++) {
      String excludeName = excludeNames.getString(i);
      RESPONSE.waterMakerExcludeNames.add(excludeName.toUpperCase());
    }

    RESPONSE.saveWithDocName = appConfig.getJsonObject("docs").getBoolean("saveWithDocName", true);
  }

  public static void internalError(RoutingContext ctx) {
    internalError(ctx, "Server error");
  }

  public static void internalError(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);
    log.error(msg);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(500);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/500");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(500);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void unauthorized(RoutingContext ctx) {
    unauthorized(ctx, "Unauthorized");
  }

  public static void unauthorized(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(401);
        response.putHeader("WWW-Authenticate", "Basic realm=\"Ad Authorization\"");
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/login");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(401);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void forbidden(RoutingContext ctx) {
    forbidden(ctx, "Forbidden");
  }

  public static void forbidden(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(403);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/403");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(403);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void badRequest(RoutingContext ctx) {
    badRequest(ctx, "Bad Request");
  }

  public static void badRequest(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    log.warn(msg);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(400);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/400");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(400);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void notFound(RoutingContext ctx) {
    notFound(ctx, "Not Found");
  }

  public static void notFound(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    log.warn(msg);

    switch (accept) {
      case "text/plain":
        response.setStatusCode(404);
        response.end(msg);
        break;
      case "text/html":
        response.setStatusCode(301);
        response.putHeader(HttpHeaders.LOCATION, "/docs-web/#/Exception/404");
        response.end();
        break;
      case "application/json":
        response.setStatusCode(404);
        response.end(JsonObject.of("success", false, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void success(RoutingContext ctx) {
    success(ctx, "Success");
  }

  public static void success(RoutingContext ctx, String msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg).encode());
        break;
      default:
        response.end(msg);
    }
  }

  public static void success(RoutingContext ctx, JsonObject msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg).encode());
        break;
      default:
        response.end(msg.encode());
    }
  }

  public static void success(RoutingContext ctx, JsonArray msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg).encode());
        break;
      default:
        response.end(msg.encode());
    }
  }

  public static void success(RoutingContext ctx, List<JsonObject> msg) {
    HttpServerResponse response = ctx.response();
    String accept = CommonUtils.getAccept(ctx);

    response.putHeader(HttpHeaders.CONTENT_TYPE, accept);

    switch (accept) {
      case "application/json":
        response.end(JsonObject.of("success", true, "msg", msg.toArray()).encode());
        break;
      default:
        response.end(msg.toArray().toString());
    }
  }

  public static void sendStatic(RoutingContext context, String requestPath) {
    final String localFilePath = FS.getLocalFile(requestPath);

    // skip hidden files
    int idx = localFilePath.lastIndexOf('/');
    String name = localFilePath.substring(idx + 1);
    if (name.length() > 0 && name.charAt(0) == '.') {
      badRequest(context);
      return;
    }

    FS.fs.props(localFilePath)
        .onFailure(err -> {
          log.debug("File does not exist or cannot be accessed: {}", localFilePath);
          notFound(context);
        })
        .onSuccess(fProps -> {
          if (fProps.isDirectory()) {
            sendDirectory(context, requestPath, localFilePath);
          } else {
            sendFile(context, localFilePath);
          }
        });
  }

  /**
   * sibling means that we are being upgraded from a directory to a index
   */
  public static void sendDirectory(RoutingContext context, String requestPath, String localFilePath) {
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

    FS.fs.readDir(localFilePath)
        .onFailure(err -> {
          log.error("Failed to read directory: {}, {}", localFilePath, err.getCause());
          internalError(context, "Failed to read directory");
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
                FileProps fProps = FS.fs.propsBlocking(s);
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

  public static void sendFile(RoutingContext context, String localFilePath) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    String ip = CommonUtils.getTrueRemoteIp(request);
    String fileName = FS.getFileName(localFilePath);

    // here user should not be null, it has been checked before
    User user = context.user();
    String loginName = user.principal().getString("login_name", "");
    String fullName = user.principal().getString("full_name", "");

    // bp info
    Session session = context.session();
    final String bpCode = Optional.ofNullable((String) session.get("BP_CODE")).orElse("");
    final String bpName = Optional.ofNullable((String) session.get("BP_NAME")).orElse("");

    // log it
    LogService.addLog("DOC_ACCESS_SUCCESS", ip, loginName, "", fileName, bpCode);

    final String wmText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
        (showCompany ? ' ' + companyName : "") +
        (showUser ? ' ' + fullName : "") +
        (showBP ? ' ' + bpName : "");

    // guess content type
    String extension = FS.getFileExtension(localFilePath);
    final String outFileName = FS.encodeFileName(
        wmText +
            (saveWithDocName
                ? ' ' + fileName
                : ' ' + CommonUtils.addRadomChar(FS.getFileNameWithoutExtension(fileName)) + " DO NOT SAVE ME"))
        + "." + extension;

    writeDateHeader(response);

    // add water mark for some file types
    if (waterMarkEnable &&
        extension != null &&
        waterMakerFileTypes.contains(extension) &&
        !CommonUtils.nameMatch(fileName, waterMakerExcludeNames)) {

      FS.fs.createTempFile(null, null)
          .onSuccess(tempFile -> {
            try {
              FileInputStream fis = new FileInputStream(localFilePath);
              FileOutputStream fos = new FileOutputStream(tempFile);

              try (fis; fos) {// auto close
                boolean b = ITextTools.addWatermark(extension, fis, fos, wmText, 30, bpCode.isEmpty() ? 0.3f : 0.03f);
                if (b) {// add watermark success
                  log.info("Add watermark to file: {}", localFilePath);
                  writeDispositionHeaders(response, outFileName + (extension.equals("pdf") ? "" : ".pdf"));

                  response.sendFile(tempFile)
                      .onComplete(ar -> {
                        FS.fs.delete(tempFile);
                      });
                } else { // failed, send original file
                  log.error("Failed to add watermark to file: {}", localFilePath);
                  writeDispositionHeaders(response, outFileName);

                  response.sendFile(localFilePath)
                      .onComplete(ar -> {
                        FS.fs.delete(tempFile);
                      });
                }

              }
            } catch (Exception e) {
              log.error("Failed to add watermark: {}", e.getCause());
              response.setStatusCode(500).end("Error adding watermark");
            }
          });

    } else { // send original file
      writeDispositionHeaders(response, outFileName);
      response.sendFile(localFilePath);
    }
  }

  /**
   * Write the date header.
   *
   * @param request
   */
  private static void writeDateHeader(HttpServerResponse response) {
    MultiMap headers = response.headers();
    // date header is mandatory
    headers.set("date", io.vertx.ext.web.impl.Utils.formatRFC1123DateTime(System.currentTimeMillis()));
  }

  /**
   * Write the content type and content disposition headers.
   *
   * @param response
   * @param outFileName
   */
  private static void writeDispositionHeaders(HttpServerResponse response, String outFileName) {
    String extension = FS.getFileExtension(outFileName);
    String contentType = Optional.ofNullable(MimeMapping.mimeTypeForExtension(extension))
        .orElse("application/octet-stream");

    if (contentType.startsWith("text")) {
      response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + defaultContentEncoding);
    } else {
      response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }
    response.putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + outFileName + "\"");
  }

}