/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-05-11 00:19:27                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-18 14:26:06                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


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

import com.da.docs.config.DocsConfig;
import com.da.docs.utils.FSUtils;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DMSServices2 {
  private static Vertx vertx = null;
  private static FileSystem fs = null;

  public static void setVertx(Vertx vertx) {
    DMSServices2.vertx = vertx;
    DMSServices2.fs = vertx.fileSystem();
  }

  private static CacheLoader<String, String> cacheLoader = new CacheLoader<String, String>() {
    @Override
    public String load(String key) {
      try {
        return doLogin().toCompletionStage().toCompletableFuture().get();
      } catch (Exception e) {
        log.error("Failed to load session from cache: {}", e.getMessage());
        return null;
      }
    }
  };
  private static RemovalListener<String, String> removalListener = (key, sessionId, cause) -> {
    log.debug("[Dms] SessionId cache {} is removed, cause is {}", sessionId, cause);
    try {
      doLogout(sessionId);
    } catch (Exception e) {
      log.error("Failed to logout session: {}", e.getMessage());
    }
  };

  /**
   * Caffeine cache for DMS session management
   */
  private static LoadingCache<String, String> dmsSessionCache = Caffeine
      .newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES)
      .removalListener(removalListener)
      .build(cacheLoader);

  /**
   * Login to DMS system, get session ID and save it to cache
   * 
   * @return
   */
  private static Future<String> doLogin() {
    String url = "http://192.168.10.64:4040/cocoon/View/LoginCAD/fr/AW_AutoLogin.html?userName=TEMP&dsn=dmsDS&Client_Type=25&computerName=AWS&LDAPControl=true";

    return HttpService2.get(url, null)
        .compose(html -> {
          String id = "";
          Pattern pattern = Pattern.compile("sSessionID = '(.*?)';");
          Matcher matcher = pattern.matcher(html);

          while (matcher.find()) {
            id = matcher.group(1);
          }

          log.debug("id: {}", id);
          return Future.succeededFuture(id);
        }).otherwise(e -> {
          log.error("Login failed: {}", e.getMessage());
          return "";
        });
  }

  /**
   * Logout from DMS system, remove session ID from cache
   * 
   * @param sessionId
   */
  private static void doLogout(String sessionId) {
    String url = "http://192.168.10.64:4040/cocoon/View/LogoutXML/fr/AW_Logout7.html?userName=TEMP&dsn=dmsDS&Client_Type=25&AUSessionID="
        + sessionId;

    HttpService2.get(url, null)
        .onFailure(e -> {
          log.error("Logout failed: {}", e.getMessage());
        });
  }

  /**
   * Encode a string to base64
   * 
   * @param value
   * @return
   */
  private static String base64Encode(String value) {
    try {
      return Base64.getEncoder().encodeToString(URLEncoder.encode(value, "UTF-8").getBytes());
    } catch (UnsupportedEncodingException e) {
      log.error(e.getMessage());
    }
    return null;
  }

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
    String date = null;
    Pattern pattern = Pattern.compile("<td data-name-attr=\"obj_modificationdate\" nowrap=\"1\">(.*?)</td>");
    Matcher matcher = pattern.matcher(htmlContent);

    while (matcher.find()) {
      date = matcher.group(1);
    }

    log.debug("modifiedDate: {}", date);
    return date;
  }

  /**
   * Get document names from DMS system
   *
   * @param Pn Project number or similar identifier, used to filter documents
   * @return Returns a list of document names
   */
  public static Future<List<String>> getDocumentNames(String Pn) {
    String sessionId = dmsSessionCache.get("SessionId");
    String search = base64Encode("%" + Pn);
    String url = String.format(
        "http://192.168.10.64:4040/cocoon/View/ExecuteService/fr/AW_AuplResult3.html?" +
            "ServiceName=aws.au&ServiceSubPackage=aws&UserName=TEMP&dsn=dmsDS&Client_Type=25&ServiceParameters=GET_AUTOCOMPLETION@%s@&AUSessionID=%s",
        search,
        sessionId);
    return HttpService2.get(url, null)
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
        }).otherwise(e -> {
          log.error("Get document names failed: {}", e.getMessage());
          return new ArrayList<String>();
        });
  }

  /**
   * Get file information from DMS system
   *
   * @param FileName Name of the file to search for
   * @return Returns the HTML content containing file information
   */
  private static Future<String> getFileInfo(String FileName) {
    String sessionId = dmsSessionCache.get("SessionId");
    String search = base64Encode(FileName);
    String url = String.format(
        "http://192.168.10.64:4040/cocoon/View/ExecuteService/fr/AW_QuickSearchView7.post?" +
            "ServiceName=aws.au&ServiceParameters=GET_OBJECTS_LIST@SEARCH@%s@@@0@9999@0@&ServiceSubPackage=aws&URL_Encoding=UTF-8&date_format=enDateHour&AUSessionID=%s",
        search,
        sessionId);

    return HttpService2.get(url, "dms")
        .compose(html -> {
          return Future.succeededFuture(html);
        }).otherwise(e -> {
          log.error("Get file info failed: {}", e.getMessage());
          return "";
        });
  }

  /**
   * Get document information from DMS system
   *
   * @param Pn Project number or similar identifier, used to filter documents
   * @return Returns a Future with a list of document information objects
   */
  public static Future<JsonArray> getDocuments(String Pn) {
    return getDocumentNames(Pn)
        .compose(documentNames -> {
          JsonArray docs = new JsonArray();
          List<Future<JsonObject>> docFutures = new ArrayList<>();

          for (String documentName : documentNames) {
            Future<JsonObject> docFuture = getFileInfo(documentName)
                .compose(html -> {
                  String fileId = extractFileId(html);
                  String dateString = extractModifiedDate(html);

                  if (dateString == null) {
                    return Future.failedFuture("DateString is null");
                  }

                  try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a", Locale.ENGLISH);
                    LocalDateTime modifiedAt = LocalDateTime.parse(dateString, formatter);

                    // Create a Docs object and set its properties
                    JsonObject doc = new JsonObject();
                    doc.put("file_name", documentName);
                    doc.put("doc_modified_at", modifiedAt.toInstant(ZoneOffset.UTC).toEpochMilli());
                    doc.put("file_id", fileId); // save id to location

                    return Future.succeededFuture(doc);
                  } catch (Exception e) {
                    log.error("Error parsing date for document {}: {}", documentName, e.getMessage());
                    return Future.failedFuture("Error parsing date for document");
                  }
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
                log.error("Error processing documents: {}", e.getMessage());
                return new JsonArray(); // return empty array on error
              });
        });
  }

  /**
   * Get document buffer from DMS system
   * 
   * @param fileId
   * @return
   */
  private static Future<Buffer> getDocumentBuffer(String fileId) {
    // 581 bugs, remove session, it works again
    String url = "http://192.168.10.64:4040/cocoon/viewDocument/ANY?FileID=" +
        fileId + "&UserName=TEMP&dsn=dmsDS&Client_Type=25";
    return HttpService2.getFile(url, "dms")
        .compose(bytes -> {
          return Future.succeededFuture(bytes);
        })
        .otherwise(e -> {
          log.error("Get document bytes failed: {}", e.getMessage());
          return Buffer.buffer();
        });
  }

  /**
   * Download documents from DMS system
   *
   * @param docs            List of documents to download
   * @param toFolder        Target folder for downloaded documents
   * @param toSubFolderDeep Depth of subFolders
   * @param toSubFolderLen  Length of subfolder names
   * @return Future that completes when all downloads are done
   */
  public static void downloadDmsDocs(
      JsonArray docs,
      String toFolder,
      int toSubFolderDeep,
      int toSubFolderLen) {

    if (fs == null) {
      DMSServices2.vertx = Vertx.vertx(DocsConfig.vertxOptions);
      fs = vertx.fileSystem();
    }
    DocsService docsService = new DocsService();

    for (Object item : docs) {
      JsonObject doc = (JsonObject) item;
      String fileName = doc.getString("file_name");

      docsService.searchDocsByName(JsonObject.of("file_name", fileName))
          .onSuccess(list -> {
            if (list.isEmpty()) {
              fs.createTempFile(null, null)
                  .onSuccess(tempFile -> {
                    log.debug("[Dms][DOWNLOAD] {} from Dms server to {}...", fileName, tempFile);

                    getDocumentBuffer(doc.getString("file_id"))
                        .onSuccess(buffer -> {
                          if (buffer.length() == 0 || buffer.length() == 581) {
                            log.error("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, buffer.length());
                            return;
                          }
                          log.info("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, buffer.length());

                          fs.writeFile(tempFile, buffer)
                              .onSuccess(v -> {
                                FSUtils.updateFileModifiedDate(tempFile, doc.getLong("doc_modified_at"));
                                FSUtils.setFileInfo(
                                    fs,
                                    tempFile,
                                    fileName,
                                    toFolder,
                                    toSubFolderDeep,
                                    toSubFolderLen,
                                    "MOVE");
                              }).onFailure(e -> {
                                log.error("[Dms][DOWNLOAD] Error writing to temp file for {}: {}", fileName,
                                    e.getMessage());
                              });
                        });
                  }).onFailure(e -> {
                    log.error("[Dms][DOWNLOAD] Error creating temp file for {}: {}", fileName, e.getMessage());
                  });
            } else {
              // Document already exists, skip downloading
              log.debug("[Dms][DOWNLOAD] {} exists, skip download", fileName);
            }
          });
    }

  }
}
