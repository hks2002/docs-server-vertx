/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-19 09:55:43                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.da.docs.annotation.GetMapping;
import com.da.docs.service.LogService;
import com.da.docs.utils.CommonUtils;
import com.da.docs.utils.ITextTools;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
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

  private String docsRoot = Utils.isWindows() ? "c:/docs" : "/mnt/docs";
  private boolean waterMarkEnable = false;
  private boolean showCompany = false;
  private boolean showBP = false;
  private boolean showUser = false;
  private String companyName = "";
  private boolean saveWithDocName = false;

  private Set<String> waterMakerFileTypes = new HashSet<>();
  private Set<String> waterMakerExcludeNames = new HashSet<>();

  public DocsHandler(JsonObject config) {
    JsonObject docsConfig = Utils.isWindows()
        ? config.getJsonObject("docs").getJsonObject("windows")
        : config.getJsonObject("docs").getJsonObject("linux");
    this.docsRoot = docsConfig.getString("docsRoot", docsRoot);

    JsonObject waterMarkConfig = config.getJsonObject("docs").getJsonObject("waterMark");
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

    this.saveWithDocName = config.getJsonObject("docs").getBoolean("saveWithDocName", true);
  }

  @Override
  public void handle(RoutingContext context) {
    User user = context.user();

    Authorizations authorizations = user.authorizations();
    if (!authorizations.verify(PermissionBasedAuthorization.create("DOCS_READ"))) {
      log.trace("{},{}", user.principal(), authorizations);
      Response.forbidden(context);
      return;
    }

    HttpServerRequest request = context.request();
    if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
      Response.badRequest(context, "Only GET and HEAD are allowed");
      return;
    } else {
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
  }

  private String getLocalFile(String requestPath, RoutingContext context) {
    String localFilePath = docsRoot + requestPath.replace("/docs-api/docs", "");
    log.trace("File to serve is " + localFilePath);
    return localFilePath;
  }

  private void sendStatic(RoutingContext context, String requestPath) {
    FileSystem fs = context.vertx().fileSystem();
    final String file = getLocalFile(requestPath, context);

    // skip hidden files
    int idx = file.lastIndexOf('/');
    String name = file.substring(idx + 1);
    if (name.length() > 0 && name.charAt(0) == '.') {
      return;
    }

    // verify if the file exists
    fs.exists(file).onFailure(err -> {
      context.fail(err);
    }).onSuccess(exists -> {
      // check again
      if (!exists) {
        Response.notFound(context);
        return;
      }

      FileProps fProps = fs.propsBlocking(file);
      if (fProps.isDirectory()) {
        sendDirectory(context, requestPath, file);
      } else {
        sendFile(context, fs, file, fProps);
      }
    });

  }

  /**
   * sibling means that we are being upgraded from a directory to a index
   */
  private void sendDirectory(RoutingContext context, String requestPath, String systemFile) {
    // in order to keep caches in a valid state we need to assert that
    // the user is requesting a directory (ends with /)
    if (!requestPath.endsWith("/")) {
      context.response()
          .putHeader(HttpHeaders.LOCATION, requestPath + "/")
          .setStatusCode(301)
          .end();
      return;
    }

    sendDirectoryListing(context, requestPath, systemFile);
  }

  private void sendDirectoryListing(RoutingContext context, String requestPath, String systemFile) {
    HttpServerResponse response = context.response();
    FileSystem fs = context.vertx().fileSystem();

    fs.readDir(systemFile).onFailure(err -> {
      context.fail(err);
    }).onSuccess(list -> {
      // log.info("{}, {}", requestPath, systemFile);

      String accept = CommonUtils.getAccept(context);
      Buffer bReturn = Buffer.buffer();

      switch (accept) {
        case "application/json":
          JsonArray json = new JsonArray();

          for (String s : list) {
            String fileName = s.substring(s.lastIndexOf(File.separatorChar) + 1);
            // skip dot files
            if (!includeHidden && fileName.charAt(0) == '.') {
              continue;
            }
            FileProps fProps = fs.propsBlocking(s);
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

          bReturn.appendBytes(json.encode().getBytes(StandardCharsets.UTF_8));
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

  private void sendFile(RoutingContext context, FileSystem fs, String file, FileProps fileProps) {
    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();
    Session session = context.session();
    LogService logService = new LogService();

    if (request.method() == HttpMethod.HEAD) {
      response.end();
    }
    writeCacheHeaders(request, fileProps);

    String fileName = getFileName(file);
    String ip = CommonUtils.getTrueRemoteIp(request);

    // here user should not be null, it has been checked before
    User user = context.user();
    String loginName = user.principal().getString("login_name", "");
    String fullName = user.principal().getString("full_name", "");

    // bp info
    final String bpCode = Optional.ofNullable((String) session.get("BP_CODE")).orElse("");
    final String bpName = Optional.ofNullable((String) session.get("BP_NAME")).orElse("");

    // log it
    logService.addLog("DOC_ACCESS_SUCCESS", ip, loginName, fullName, fileName, bpCode, bpName);

    final String wmText = (showCompany ? companyName : "") +
        (showUser ? ' ' + fullName : "") +
        (showBP ? ' ' + bpName : "") +
        ' ' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    // guess content type
    String extension = getFileExtension(file);
    String contentType = MimeMapping.mimeTypeForExtension(extension);
    final String outFileName = encodeFileName(wmText +
        (saveWithDocName ? ' ' + fileName : " DO NOT SAVE ME")) +
        (extension.equals("pdf") ? ".PDF" : "." + extension + ".PDF");

    // add water mark for some file types
    if (waterMarkEnable &&
        extension != null &&
        waterMakerFileTypes.contains(extension) &&
        !CommonUtils.nameMatch(fileName, waterMakerExcludeNames)) {

      // add watermark
      fs.readFile(file).onSuccess(buffer -> {
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer.getBytes());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        context.vertx().executeBlocking(
            () -> {
              return ITextTools.addWatermark(extension, bis, bos, wmText, 30, bpCode.isEmpty() ? 0.3f : 0.03f);
            }).onSuccess(succeed -> {
              if (!succeed) {
                log.error("Failed to add watermark to file: {}", file);
                writeDispositionHeaders(response, MimeMapping.mimeTypeForExtension("pdf"), outFileName);
                Buffer responseBuffer = Buffer.buffer(bos.toByteArray());
                response.end(responseBuffer);
              } else {
                writeDispositionHeaders(response, MimeMapping.mimeTypeForExtension(extension), outFileName);
                Buffer responseBuffer = Buffer.buffer(buffer.getBytes());
                response.end(responseBuffer);
              }
            }).onFailure(err -> {
              log.error("Error while adding watermark to file: {}", file, err);
              writeDispositionHeaders(response, MimeMapping.mimeTypeForExtension(extension), outFileName);
              Buffer responseBuffer = Buffer.buffer(buffer.getBytes());
              response.end(responseBuffer);
            });
      });

    } else { // send original file
      writeDispositionHeaders(response, contentType, outFileName);

      fs.readFile(file).onSuccess(buffer -> {
        Buffer responseBuffer = Buffer.buffer(buffer.getBytes());
        response.end(responseBuffer);
      });

    }

  }

  /**
   * Create all required header so content can be cache by Caching servers or
   * Browsers
   *
   * @param request base HttpServerRequest
   * @param props   file properties
   */
  private void writeCacheHeaders(HttpServerRequest request, FileProps props) {

    MultiMap headers = request.response().headers();
    // date header is mandatory
    headers.set("date", io.vertx.ext.web.impl.Utils.formatRFC1123DateTime(System.currentTimeMillis()));
  }

  private void writeDispositionHeaders(HttpServerResponse response, String contentType, String outFileName) {
    if (contentType.startsWith("text")) {
      response.putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + defaultContentEncoding);
    } else {
      response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }
    response.putHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + outFileName + "\"");
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

  private String getFileName(String file) {
    int lastSeparatorIndex = file.lastIndexOf('/');
    if (lastSeparatorIndex == -1) {
      lastSeparatorIndex = file.lastIndexOf('\\');
    }

    if (lastSeparatorIndex != -1 && lastSeparatorIndex < file.length() - 1) {
      return file.substring(lastSeparatorIndex + 1);
    } else {
      return file;
    }
  }

  private static String encodeFileName(String fileName) {
    try {
      String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());
      return encodedFileName.replace("+", "%20");
    } catch (Exception e) {
      return fileName;
    }
  }

}
