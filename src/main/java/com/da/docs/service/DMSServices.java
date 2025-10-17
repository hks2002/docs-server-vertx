/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-05-11 00:19:27                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-17 16:58:22                                                                      *
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

import com.da.docs.VertxHolder;
import com.da.docs.utils.FSUtils;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DMSServices {

  private static String dmsServer = VertxHolder.appConfig == null ? new String()
      : VertxHolder.appConfig.getString("dmsServer");

  public void setDmsServer(String server) {
    DMSServices.dmsServer = server;
  }

  private static CacheLoader<String, String> cacheLoader = new CacheLoader<String, String>() {
    @Override
    public String load(String key) {
      try {
        return doLogin().toCompletionStage().toCompletableFuture().get();
      } catch (Exception e) {
        log.error("DMS Login error: {}", e.getCause());
        return "";
      }
    }
  };
  private static RemovalListener<String, String> removalListener = (key, sessionId, cause) -> {
    log.debug(
        "[Dms] SessionId cache {} is removed, cause is {}",
        sessionId,
        cause);
    doLogout(sessionId);
  };

  private static LoadingCache<String, String> dmsSessionCache = Caffeine
      .newBuilder()
      .expireAfterAccess(15, TimeUnit.MINUTES)
      .removalListener(removalListener)
      .build(cacheLoader);

  /**
   * Login to DMS system, get session ID
   * 
   * @return
   */
  public static Future<String> doLogin() {
    String url = dmsServer
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
        }).onFailure(e -> {
          log.error("Login failed: {}", e.getCause());
        });
  }

  /**
   * Logout from DMS system, remove session ID from cache
   * 
   * @param sessionId
   */
  private static void doLogout(String sessionId) {
    String url = dmsServer
        + "/cocoon/View/LogoutXML/fr/AW_Logout7.html?userName=TEMP&dsn=dmsDS&Client_Type=25&AUSessionID="
        + sessionId;

    HttpService.get(url).onSuccess(res -> {
      log.debug("Logout success: {}", sessionId);
    })
        .onFailure(e -> {
          log.error("Logout failed: {}", e.getCause());
        });
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
   * @param Pn Project number or similar identifier, used to filter documents
   * @return Returns a list of document names
   */
  private static Future<List<String>> getDocumentNames(String sessionId, String Pn) {
    String search = base64Encode("%" + Pn);

    String url = String.format(
        dmsServer + "/cocoon/View/ExecuteService/fr/AW_AuplResult3.html?" +
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
        }).otherwise(e -> {
          log.error("Get document names failed: {}", e.getCause());
          return new ArrayList<String>();
        });
  }

  /**
   * Get file information from DMS system
   *
   * @param FileName Name of the file to search for
   * @return Returns the HTML content containing file information
   */
  private static Future<String> getFileInfo(String sessionId, String FileName) {
    String search = base64Encode(FileName);

    String url = String.format(
        dmsServer + "/cocoon/View/ExecuteService/fr/AW_QuickSearchView7.post?" +
            "ServiceName=aws.au&ServiceParameters=GET_OBJECTS_LIST@SEARCH@%s@@@0@9999@0@&ServiceSubPackage=aws&URL_Encoding=UTF-8&date_format=enDateHour&AUSessionID=%s",
        search,
        sessionId);

    return HttpService.get(url)
        .compose(html -> {
          return Future.succeededFuture(html);
        }).otherwise(e -> {
          log.error("Get file info failed: {}", e.getCause());
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
    String sessionId = dmsSessionCache.getIfPresent("dmsSession");

    return getDocumentNames(sessionId, Pn)
        .compose(documentNames -> {
          JsonArray docs = new JsonArray();
          List<Future<JsonObject>> docFutures = new ArrayList<>();

          for (String documentName : documentNames) {
            Future<JsonObject> docFuture = getFileInfo(sessionId, documentName)
                .compose(html -> {
                  String fileId = extractFileId(html);
                  String dateString = extractModifiedDate(html);

                  if (dateString == null) {
                    return Future.failedFuture("DateString is null");
                  }

                  try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a", Locale.ENGLISH);
                    LocalDateTime modifiedAt = LocalDateTime.parse(dateString, formatter);
                    long modifiedAtEpoch = modifiedAt.toInstant(ZoneOffset.UTC).toEpochMilli();

                    // Create a Docs object and set its properties
                    JsonObject doc = new JsonObject();
                    doc.put("file_name", documentName);
                    doc.put("doc_modified_at", modifiedAtEpoch);
                    doc.put("file_id", fileId);
                    downloadDmsDocs(documentName, fileId, modifiedAtEpoch);

                    return Future.succeededFuture(doc);
                  } catch (Exception e) {
                    log.error("Error parsing date for document {}: {}", documentName, e.getCause());
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
                log.error("Error processing documents: {}", e.getCause());
                e.printStackTrace();
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
    String url = dmsServer + "/cocoon/viewDocument/ANY?FileID=" +
        fileId + "&UserName=TEMP&dsn=dmsDS&Client_Type=25";

    return HttpService.getFile(url)
        .compose(bytes -> {
          return Future.succeededFuture(bytes);
        })
        .otherwise(e -> {
          log.error("Get document bytes failed: {}", e.getCause());
          return Buffer.buffer();
        });
  }

  /**
   * Download documents from DMS system
   *
   * @param docs List of documents to download
   */
  public static void downloadDmsDocs(String fileName, String fileId, Long modifiedAt) {
    if (FSUtils.isFileExists(fileName)) {
      log.debug("[Dms][DOWNLOAD] {} exists, skip download", fileName);
      return;
    }
    log.info("[Dms][DOWNLOAD] start download {} from Dms server", fileName);

    getDocumentBuffer(fileId)
        .onSuccess(buffer -> {
          if (buffer.length() == 0 || buffer.length() == 581) {
            log.error("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, buffer.length());
            return;
          }
          log.info("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, buffer.length());

          VertxHolder.fs.createTempFile(null, null)
              .onSuccess(tempFile -> {
                VertxHolder.fs.writeFile(tempFile, buffer)
                    .onSuccess(v -> {
                      FSUtils.updateFileModifiedDate(tempFile, modifiedAt);
                      FSUtils.setFileInfo(
                          tempFile,
                          fileName,
                          fileId,
                          "MOVE")
                          .onSuccess(res -> {
                            log.info("[Dms][DOWNLOAD] Successfully downloaded and processed file: {}", fileName);
                          })
                          .onFailure(e -> {
                            log.error("[Dms][DOWNLOAD] Error processing file {}: {}", fileName, e.getCause());
                          });
                    })
                    .onFailure(e -> {
                      log.error("[Dms][DOWNLOAD] Error writing to temp file for {}: {}", fileName, e.getCause());
                    });
              });

        })
        .onFailure(e -> {
          log.error("[Dms][DOWNLOAD] Error downloading file {}: {}", fileName, e.getCause());
        });
  }
}
