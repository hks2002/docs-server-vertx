/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-05-11 00:19:27                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 19:57:10                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.da.docs.VertxApp;
import com.da.docs.utils.FSUtils;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DMSServices {
  private static String dmsServer = null;
  private static String dmsServerFast = null;
  private static FileSystem fs = null;

  public DMSServices() {
    dmsServer = VertxApp.appConfig.getString("dmsServer");
    dmsServerFast = VertxApp.appConfig.getString("dmsServerFast");
    fs = VertxApp.fs;
  }

  public static void setup(String server, String serverFast, FileSystem fileSystem) {
    dmsServer = server;
    dmsServerFast = serverFast;
    fs = fileSystem;
  }

  /**
   * Login to DMS system, get session ID
   *
   * @param server DMS server
   * @return
   */
  public static Future<String> doLogin(String server) {
    String url = server
        + "/cocoon/View/LoginCAD/fr/AW_AutoLogin.html?userName=TEMP&dsn=dmsDS&Client_Type=25&computerName=AWS&LDAPControl=true";

    return HttpService.get(url)
        .compose(html -> {
          String id = "";
          Pattern pattern = Pattern.compile("sSessionID = '(.*?)';");
          Matcher matcher = pattern.matcher(html);

          while (matcher.find()) {
            id = matcher.group(1);
          }

          log.debug("id: {}", id);
          return Future.succeededFuture(id);
        })
        .onFailure(e -> {
          log.error("Login failed: {}", e.getCause());
        });
  }

  /**
   * Logout from DMS system, remove session ID from cache
   *
   * @param server    DMS server
   * @param sessionId Current session ID
   */
  private static void doLogout(String server, String sessionId) {
    String url = server
        + "/cocoon/View/LogoutXML/fr/AW_Logout7.html?userName=TEMP&dsn=dmsDS&Client_Type=25&AUSessionID="
        + sessionId;

    HttpService.get(url)
        .onSuccess(res -> {
          log.debug("Logout success: {}", sessionId);
        })
        .onFailure(e -> {
          log.error("Logout failed: {}", e.getCause());
        });
  }

  // Cache loader for DMS session
  private static CacheLoader<String, String> cacheLoader = new CacheLoader<String, String>() {
    @Override
    public String load(String server) {
      try {
        return doLogin(server).toCompletionStage().toCompletableFuture().get();
      } catch (Exception e) {
        log.error("DMS Login error: {}", e.getCause());
        return "";
      }
    }
  };

  // Removal listener for DMS session
  private static RemovalListener<String, String> removalListener = (server, sessionId, cause) -> {
    log.debug("[Dms] SessionId cache {} is removed, cause is {}", sessionId, cause);
    doLogout(server, sessionId);
  };

  // DMS session cache
  private static LoadingCache<String, String> dmsSessionCache = Caffeine
      .newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .removalListener(removalListener)
      .build(cacheLoader);

  /**
   * Extract
   * <LI>values from XML content
   *
   * @param xmlContent
   * @return
   */
  private static List<String> extractLIValues(String xmlContent) {
    List<String> liValues = new ArrayList<>();
    Pattern pattern = Pattern.compile("<LI>(.*?)</LI>");
    Matcher matcher = pattern.matcher(xmlContent);

    while (matcher.find()) {
      liValues.add(matcher.group(1));
    }

    log.debug("liValues: {}", liValues);
    return liValues;
  }

  /**
   * Extract file ID from HTML content
   *
   * @param htmlContent
   * @return
   */
  private static String extractFileId(String htmlContent) {
    Pattern pattern = Pattern.compile("var id\t\t= '(.*?)';");
    Matcher matcher = pattern.matcher(htmlContent);
    String id = null;
    while (matcher.find()) {
      id = matcher.group(1);
    }
    log.debug("fileId: {}", id);
    return id;
  }

  /**
   * Extract modified date from HTML content
   *
   * @param htmlContent
   * @return
   */
  private static String extractModifiedDate(String htmlContent) {
    log.debug("extractModifiedDate\n{}", htmlContent);
    String date = null;
    Pattern pattern = Pattern.compile("<td data-name-attr=\"obj_modificationdate\" nowrap=\"1\">(.*?)</td>");
    Matcher matcher = pattern.matcher(htmlContent);

    while (matcher.find()) {
      date = matcher.group(1);
    }

    log.debug("modifiedDate: {}", date);
    return date;
  }

  private static String base64Encode(String value) {
    try {
      return Base64.getEncoder().encodeToString(URLEncoder.encode(value, "UTF-8").getBytes());
    } catch (UnsupportedEncodingException e) {
      log.error(e.getCause());
    }
    return null;
  }

  /**
   * Get document names from DMS system
   *
   * @param server    DMS server
   * @param sessionId Current session ID
   * @param Pn        Project number or similar identifier, used to filter
   *                  documents
   * @return Returns a list of document names
   */
  private static Future<List<String>> getDocumentNames(String server, String sessionId, String Pn) {
    String search = base64Encode("%" + Pn);

    String url = String.format(
        server + "/cocoon/View/ExecuteService/fr/AW_AuplResult3.html?" +
            "ServiceName=aws.au&ServiceSubPackage=aws&UserName=TEMP&dsn=dmsDS&Client_Type=25&ServiceParameters=GET_AUTOCOMPLETION@%s@&AUSessionID=%s",
        search,
        sessionId);

    return HttpService.get(url)
        .compose(xml -> {
          List<String> liValues = extractLIValues(xml);
          List<String> result = new ArrayList<>();

          for (int i = 0; i < liValues.size(); i++) {
            // only files with extension
            if (liValues.get(i).indexOf('.') == -1) {
              continue;
            }
            result.add(liValues.get(i));
          }
          log.debug("Document names: {}", result);
          return Future.succeededFuture(result);
        }).recover(e -> {
          log.error("Get document names failed: {}", e.getCause());
          return Future.failedFuture(e.getMessage());
        });
  }

  /**
   * Get file information from DMS system
   *
   * @param server    DMS server
   * @param sessionId Current session ID
   * @param FileName  Name of the file to search for
   * @return Returns the HTML content containing file information
   */
  private static Future<String> getFileInfo(String server, String sessionId, String FileName) {
    String search = base64Encode(FileName);

    String url = String.format(
        server + "/cocoon/View/ExecuteService/fr/AW_QuickSearchView7.post?" +
            "ServiceName=aws.au&ServiceParameters=GET_OBJECTS_LIST@SEARCH@%s@@@0@9999@0@&ServiceSubPackage=aws&URL_Encoding=UTF-8&date_format=enDateHour&AUSessionID=%s",
        search,
        sessionId);

    return HttpService.get(url)
        .compose(html -> {
          return Future.succeededFuture(html);
        })
        .recover(e -> {
          log.error("Get file info failed: {}", e.getCause());
          return Future.failedFuture(e.getMessage());
        });
  }

  /**
   * Get document buffer from DMS system, using fast server first, if fails, try
   * normal server
   *
   * @param fileId   The ID of the file to be downloaded
   * @param fileName The name of the file to be downloaded
   * @return
   */
  private static Future<Buffer> getDocumentBuffer(String fileId, String fileName) {

    // 581 bugs, means login required, remove session, it works again
    String url = dmsServer + "/cocoon/viewDocument/ANY?FileID=" + fileId + "&UserName=TEMP&dsn=dmsDS&Client_Type=25";
    String urlFast = dmsServerFast + "/cocoon/viewDocument/ANY?FileID=" + fileId
        + "&UserName=TEMP&dsn=dmsDS&Client_Type=25";

    return HttpService.getFile(urlFast)
        .compose(bytes -> {
          if (bytes.length() == 0 || bytes.length() == 581) {
            dmsSessionCache.invalidate(dmsServerFast);
            log.error("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, bytes.length());
            return Future.failedFuture("Invalid file size");
          }
          return Future.succeededFuture(bytes);
        })
        .recover(ar -> {
          // retry once
          return HttpService.getFile(url)
              .compose(bytes -> {
                if (bytes.length() == 0 || bytes.length() == 581) {
                  dmsSessionCache.invalidate(dmsServer);
                  log.error("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, bytes.length());
                  return Future.failedFuture("Invalid file size");
                }
                return Future.succeededFuture(bytes);
              });
        });
  }

  /**
   * Get document information from DMS system
   *
   * @param Pn Project number or similar identifier, used to filter documents
   * @return Returns a Future with a list of document information objects
   */
  public static Future<JsonArray> getDocuments(String Pn) {
    String sessionId = dmsSessionCache.getIfPresent(dmsServer);
    dmsSessionCache.getIfPresent(dmsServerFast); // make the fast server session cached too, so that fast download works

    return getDocumentNames(dmsServer, sessionId, Pn)
        .compose(documentNames -> {
          JsonArray docs = new JsonArray();
          List<Future<JsonObject>> docFutures = new ArrayList<>();

          for (String documentName : documentNames) {
            Future<JsonObject> docFuture = getFileInfo(dmsServer, sessionId, documentName)
                .compose(html -> {
                  String fileId = extractFileId(html);
                  String dateString = extractModifiedDate(html);

                  if (dateString == null) {
                    return Future.failedFuture("DateString is null");
                  }

                  LocalDateTime modifiedAt = null;
                  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a", Locale.ENGLISH);
                  try {
                    modifiedAt = LocalDateTime.parse(dateString, formatter);
                  } catch (Exception e) {
                    log.error("Error parsing date for document {}: {}", documentName, e.getMessage());
                    return Future.failedFuture("Error parsing date for document");
                  }
                  long modifiedAtEpoch = modifiedAt.toInstant(ZoneOffset.UTC).toEpochMilli();

                  // Create a Docs object and set its properties
                  JsonObject doc = new JsonObject();
                  doc.put("file_name", documentName);
                  doc.put("doc_modified_at", modifiedAtEpoch);
                  doc.put("file_id", fileId);

                  // Download the document and save to database
                  downloadDmsDocs(documentName, fileId, modifiedAtEpoch);

                  return Future.succeededFuture(doc);
                }).compose(v -> {
                  // only return successful docs
                  return Future.succeededFuture(v);
                });

            docFutures.add(docFuture);
          }

          // When all futures complete, collect results into docs array
          return Future.all(docFutures)
              .map(composite -> {
                for (int i = 0; i < composite.size(); i++) {
                  JsonObject doc = composite.resultAt(i);
                  docs.add(doc);
                }
                return docs;
              }).otherwise(e -> {
                log.error("Error processing documents: {}", e.getCause());
                e.printStackTrace();
                return new JsonArray(); // return empty array on error
              });
        });

  }

  /**
   * Download documents from DMS system
   *
   * @param fileName   the file name
   * @param fileId     the file ID in DMS system
   * @param modifiedAt the modified date
   * @return Future with temp file path
   */
  public static Future<Void> downloadDmsDocs(String fileName, String fileId, Long modifiedAt) {
    // get the destination folder
    String toSubFolder = FSUtils.getFolderPathByFileName(fileName);
    String toFolderFullPath = FSUtils.getDocsRoot() + '/' + toSubFolder;
    String toFileFullPath = toFolderFullPath + '/' + fileName;

    if (FSUtils.isFileExists(toFileFullPath)) {
      log.debug("[Dms][DOWNLOAD] {} exists, skip download", fileName);
      return Future.succeededFuture();
    }
    log.info("[Dms][DOWNLOAD] start download {} from Dms server", fileName);

    return Future.all(fs.createTempFile(null, null), getDocumentBuffer(fileId, fileName))
        .compose(ar -> {
          String tempFile = ar.resultAt(0);
          Buffer buffer = ar.resultAt(1);
          log.info("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, buffer.length());

          return fs.writeFile(tempFile, buffer)
              .compose(v -> {
                FSUtils.updateFileModifiedDate(tempFile, modifiedAt);
                return Future.succeededFuture();
              }).compose(v -> {
                return DocsService.moveFile(tempFile, fileName, "MOVE");
              }).andThen(v3 -> {
                DocsService.addFileInfo(fileName, fileId);
              });
        });
  }

}
