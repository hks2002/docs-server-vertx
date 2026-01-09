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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.da.docs.VertxApp;
import com.da.docs.pojo.Attachment;
import com.da.docs.pojo.Attachments;
import com.da.docs.pojo.ResultData;
import com.da.docs.utils.FSUtils;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DMSServices {
  // Static class, must be initialized, be careful the null value
  private static String dmsServer = VertxApp.appConfig == null ? "" : VertxApp.appConfig.getString("dmsServer");
  private static String dmsServerFast = VertxApp.appConfig == null ? "" : VertxApp.appConfig.getString("dmsServerFast");
  private static FileSystem fs = VertxApp.fs == null ? Vertx.vertx().fileSystem() : VertxApp.fs;

  public static void setup(String server, String serverFast, FileSystem fileSystem) {
    dmsServer = server;
    dmsServerFast = serverFast;
    fs = fileSystem;
  }

  /**
   * Login to DMS system, get session ID
   *
   * @param server DMS server
   * @return Future with session ID
   */
  public static Future<String> doLogin(String server) {
    String url = server
        + "/cocoon/View/LoginCAD/fr/AW_AutoLogin.html?userName=TEMP&dsn=dmsDS&Client_Type=25&computerName=AWS&LDAPControl=true";

    return HttpService.get(url)
        .compose(html -> {
          log.info("Login success, received HTML response");

          String id = "";
          Pattern pattern = Pattern.compile("sSessionID = '(.*?)';");
          Matcher matcher = pattern.matcher(html);

          while (matcher.find()) {
            id = matcher.group(1);
          }

          log.debug("id: {}", id);
          return Future.succeededFuture(id);
        })
        .recover(throwable -> {
          log.error("Login failed: {}", throwable.getMessage());
          return Future.succeededFuture("");
        });
  }

  /**
   * Logout from DMS system, remove session ID from cache
   *
   * @param server    DMS server
   * @param sessionId Current session ID
   */
  private static Future<Void> doLogout(String server, String sessionId) {
    if (sessionId == null || sessionId.isEmpty()) {
      return Future.succeededFuture(); // nothing to do
    }

    String url = server
        + "/cocoon/View/LogoutXML/fr/AW_Logout7.html?userName=TEMP&dsn=dmsDS&Client_Type=25&AUSessionID="
        + sessionId;

    return HttpService.get(url)
        .onSuccess(res -> {
          log.debug("Logout success: {}", sessionId);
        })
        .onFailure(e -> {
          log.error("Logout failed: {}", e.getCause());
        })
        .mapEmpty();
  }

  // Cache loader for DMS session
  private static final AsyncCacheLoader<String, String> asyncCacheLoader = (key, executor) -> {
    return doLogin(key).toCompletionStage().toCompletableFuture();
  };

  // Removal listener for DMS session
  private static RemovalListener<String, String> removalListener = (server, sessionId, cause) -> {
    log.debug("[Dms] SessionId cache {} is removed, cause is {}", sessionId, cause);
    doLogout(server, sessionId);
  };

  // DMS session cache
  private static final AsyncLoadingCache<String, String> dmsSessionCache = Caffeine
      .newBuilder()
      .expireAfterAccess(10, TimeUnit.MINUTES)
      .removalListener(removalListener)
      .buildAsync(asyncCacheLoader);

  private static String btoa(String str) {
    return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
  }

  private static String encodeURIComponent(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8")
          // JS的encodeURIComponent不会编码这些字符：! ' ( ) ~, 恢复这些值
          .replace("+", "%20")
          .replace("%21", "!")
          .replace("%27", "'")
          .replace("%28", "(")
          .replace("%29", ")")
          .replace("%7E", "~");
    } catch (UnsupportedEncodingException e) {
      log.error(e.getMessage());
    }
    return null;
  }

  public static String au_URLEncode(String str) {
    try {
      return btoa(encodeURIComponent(str));
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return null;
  }

  /**
   * Get object ids by attachment file name from DMS system
   *
   * @param server    DMS server
   * @param sessionId Current session ID
   * @param filename  Filename used to filter
   *                  documents
   * @return Returns a list of document object IDs
   */
  private static Future<List<String>> getObjectIds(String server, String sessionId, String filename) {
    String search = "UPPER(F.nomfich) LIKE '*" + filename.toUpperCase() + "*'";
    String search_encode = au_URLEncode(search);

    String url = String.format(server
        + "/cocoon/View/ExecuteService.xml?"
        + "ServiceName=aws.au"
        + "&ServiceSubPackage=aws"
        + "&ServiceParameters=EVALUATE_SEARCH_CONTEXT"
        + "@DOCUMENT"
        + "@%s"
        + "@@@@0@@0@"
        + "&AUSessionID=%s",
        search_encode,
        sessionId);

    return HttpService.get(url)
        .compose(xml -> {
          XmlMapper xmlMapper = new XmlMapper();
          try {
            ResultData resultData = xmlMapper.readValue(xml, ResultData.class);
            log.debug("Get object ids by file name response: {}", resultData);

            if (resultData.getResults().isEmpty()) {
              return Future.succeededFuture(new ArrayList<>());
            }

            int resultCode = resultData.getResults().get(0).getResultCode();
            if (resultCode != 0) {
              return Future.failedFuture("DMS service search error: " + resultCode);
            }

            String resultDetail = resultData.getResults().get(0).getResultDetail();
            String[] parts = resultDetail.split("\\|");
            List<String> fileIds = new ArrayList<>(Arrays.asList(parts));
            fileIds.remove(0);

            log.debug("Get object ids by file name succeeded: {}", fileIds);
            return Future.succeededFuture(fileIds);
          } catch (Exception e) {
            log.error("Get object ids failed: {}", e.getMessage());
            return Future.failedFuture(e.getMessage());
          }

        });
  }

  private static Future<List<JsonObject>> getAttachmentInfos(String server, String sessionId, String objectId) {
    String url = String.format(server
        + "/cocoon/View/Attachments.xml?"
        + "UserName=TEMP"
        + "&dsn=dmsDS"
        + "&Client_Type=25"
        + "&ProjectContextID=9999"
        + "&objectID=%s"
        + "&AUSessionID=%s",
        objectId, sessionId);

    return HttpService.get(url)
        .compose(xml -> {

          XmlMapper xmlMapper = new XmlMapper();
          Attachments attachments = null;
          try {
            attachments = xmlMapper.readValue(xml, Attachments.class);
          } catch (Exception e) {
            log.error("Get attachment infos failed: {}", e.getMessage());
            return Future.failedFuture(e.getMessage());
          }
          log.debug("Get attachment infos response: {}", attachments);

          // this formatter is decided by DMS system server settings only
          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH);
          List<JsonObject> result = new ArrayList<>();

          for (Attachment a : attachments.getAttachments()) {
            Long fileId = a.getId();
            String dateString = a.getDateModif();
            String documentName = a.getName();

            LocalDateTime modifiedAt = null;
            // skip null modified date attachment
            if (dateString == null) {
              break;
            }

            try {
              modifiedAt = LocalDateTime.parse(dateString, formatter);
            } catch (Exception e) {
              log.error("Error parsing date for document {}: {}", documentName, e.getMessage());
              break;
            }
            long modifiedAtEpoch = modifiedAt.toInstant(ZoneOffset.UTC).toEpochMilli();

            // Create a Docs object and set its properties
            JsonObject doc = new JsonObject();
            doc.put("name", documentName);
            doc.put("lastModified", modifiedAtEpoch);
            doc.put("size", a.getSize());
            doc.put("format", a.getFormat());
            doc.put("objectId", objectId);
            doc.put("fileId", fileId);

            log.debug("Document processed: {}", doc);
            result.add(doc);
          }
          log.debug("Get attachment infos succeeded: {}", result);
          return Future.succeededFuture(result);

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
    String url = dmsServer
        + "/cocoon/viewDocument/ANY?"
        + "FileID=" + fileId
        + "&UserName=TEMP"
        + "&dsn=dmsDS"
        + "&Client_Type=25";
    String urlFast = dmsServerFast
        + "/cocoon/viewDocument/ANY?"
        + "FileID=" + fileId
        + "&UserName=TEMP"
        + "&dsn=dmsDS"
        + "&Client_Type=25";

    return HttpService.getFile(urlFast)
        .compose(bytes -> {
          if (bytes.length() == 0 || bytes.length() == 581) {
            dmsSessionCache.synchronous().invalidate(dmsServerFast);
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
                  dmsSessionCache.synchronous().invalidate(dmsServer);
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
    Future<String> f1 = Future.fromCompletionStage(dmsSessionCache.get(dmsServer));
    dmsSessionCache.get(dmsServerFast);

    return f1.compose(sessionId -> {
      return getObjectIds(dmsServer, sessionId, Pn)
          .compose(objIds -> {
            // if no object ids, return empty array immediately
            if (objIds == null || objIds.isEmpty()) {
              return Future.succeededFuture(new JsonArray());
            }

            // collect futures that retrieve attachment lists for each object id
            List<Future<List<JsonObject>>> attachmentFutures = new ArrayList<>();
            for (String objId : objIds) {
              log.debug("Fetching attachment info for object ID: {} {}", objId, sessionId);
              attachmentFutures.add(getAttachmentInfos(dmsServer, sessionId, objId));
            }

            // When all attachment futures complete, aggregate all sub-lists into a single
            // JsonArray
            return Future.all(attachmentFutures)
                .map(composite -> {
                  JsonArray docs = new JsonArray();
                  for (int i = 0; i < composite.size(); i++) {
                    @SuppressWarnings("unchecked")
                    List<JsonObject> subList = (List<JsonObject>) composite.resultAt(i);
                    if (subList != null) {
                      for (JsonObject doc : subList) {
                        docs.add(doc);
                      }
                    }
                  }
                  return docs;
                }).otherwise(e -> {
                  log.error("Error processing documents: {}", e.getMessage());
                  return new JsonArray(); // return empty array on error
                });
          });
    });
  }

  /**
   * Check before download documents from DMS system
   *
   * @param fileName            the file name
   * @param modifiedAtToCompare the modified date
   * @return SKIP or START
   */
  public static Future<String> downloadDmsDocsCheck(String fileName, Long modifiedAtToCompare) {
    return Future.all(FSUtils.getFolderPathByFileName(fileName), FSUtils.getDocsRoot()).compose(data -> {
      String toSubFolder = data.resultAt(0);
      String docRoot = data.resultAt(1);
      String toFolderFullPath = docRoot + '/' + toSubFolder;
      String toFileFullPath = toFolderFullPath + '/' + fileName;

      return FSUtils.isFileExists(toFileFullPath).compose(fileExists -> {
        if (fileExists) {
          return FSUtils.getFileModifiedTime(toFileFullPath)
              .compose(modifiedTime -> {
                if (modifiedTime >= modifiedAtToCompare) {
                  log.debug("[Dms][DOWNLOAD] {} exists and is up-to-date, skip download", fileName);
                  return Future.succeededFuture("SKIP");
                } else {
                  return fs.delete(toFileFullPath).compose(v -> {
                    log.info("[Dms][DOWNLOAD] {} exists and is outdated, re-downloading", fileName);
                    return Future.succeededFuture("REDOWNLOAD");
                  });

                }

              });
        } else {
          return Future.succeededFuture("START");
        }
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
  public static Future<String> downloadDmsDocs(String fileName, String fileId, Long modifiedAt) {
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
                return new DocsService().moveFile(tempFile, fileName, "MOVE");
              }).andThen(v3 -> {
                new DocsService().addFileInfo(fileName, fileId);
              }).compose(v -> {
                return Future.succeededFuture("DOWNLOADED");
              });
        });
  }

}
