/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-05-11 00:19:27                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-13 11:59:22                                                                      *
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.da.docs.utils.FSUtils;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class DMSServices {
  private static CacheLoader<String, String> cacheLoader = new CacheLoader<String, String>() {
    @Override
    public String load(String key) {
      return doLogin();
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

  private static String doLogin() {
    String html = HttpService
        .request(
            "http://192.168.10.64:4040/cocoon/View/LoginCAD/fr/AW_AutoLogin.html?userName=TEMP&dsn=dmsDS&Client_Type=25&computerName=AWS&LDAPControl=true",
            "GET",
            null,
            null)
        .body();

    String id = "";
    Pattern pattern = Pattern.compile("sSessionID = '(.*?)';");
    Matcher matcher = pattern.matcher(html);

    while (matcher.find()) {
      id = matcher.group(1);
    }

    log.debug("id: {}", id);
    return id;
  }

  private static void doLogout(String sessionId) {
    HttpService
        .request(
            "http://192.168.10.64:4040/cocoon/View/LogoutXML/fr/AW_Logout7.html?userName=TEMP&dsn=dmsDS&Client_Type=25&AUSessionID="
                +
                sessionId,
            "GET",
            null,
            null)
        .body();
  }

  private static String base64Encode(String value) {
    try {
      return Base64.getEncoder().encodeToString(URLEncoder.encode(value, "UTF-8").getBytes());
    } catch (UnsupportedEncodingException e) {
      log.error(e.getMessage());
    }
    return null;
  }

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
  private static List<String> getDocumentNames(String Pn) {
    List<String> docs = new ArrayList<>();
    String sessionId = dmsSessionCache.get("SessionId");
    String search = base64Encode("%" + Pn);
    String url = String.format(
        "http://192.168.10.64:4040/cocoon/View/ExecuteService/fr/AW_AuplResult3.html?" +
            "ServiceName=aws.au&ServiceSubPackage=aws&UserName=TEMP&dsn=dmsDS&Client_Type=25&ServiceParameters=GET_AUTOCOMPLETION@%s@&AUSessionID=%s",
        search,
        sessionId);
    String xml = HttpService.request(url, "GET", null, null).body();
    List<String> liValues = extractLIValues(xml);

    for (int i = 0; i < liValues.size(); i++) {
      // only files with extension
      if (liValues.get(i).indexOf('.') == -1) {
        continue;
      }
      docs.add(liValues.get(i));
    }

    return docs;
  }

  /**
   * Get file information from DMS system
   *
   * @param FileName Name of the file to search for
   * @return Returns the HTML content containing file information
   */
  private static String getFileInfo(String FileName) {
    String sessionId = dmsSessionCache.get("SessionId");
    String search = base64Encode(FileName);
    String url = String.format(
        "http://192.168.10.64:4040/cocoon/View/ExecuteService/fr/AW_QuickSearchView7.post?" +
            "ServiceName=aws.au&ServiceParameters=GET_OBJECTS_LIST@SEARCH@%s@@@0@9999@0@&ServiceSubPackage=aws&URL_Encoding=UTF-8&date_format=enDateHour&AUSessionID=%s",
        search,
        sessionId);
    return HttpService.request(url, "GET", null, null).body();
  }

  private static byte[] getDocumentBytes(String id) {
    // 581 bugs, remove session, it works again
    return HttpService.getFile(
        "http://192.168.10.64:4040/cocoon/viewDocument/ANY?FileID=" +
            id +
            "&UserName=TEMP&dsn=dmsDS&Client_Type=25");
  }

  /**
   * Get document information from DMS system
   *
   * @param Pn Project number or similar identifier, used to filter documents
   * @return Returns a list of document information objects
   */
  public static JsonArray getDocuments(String Pn) {
    List<String> documentNames = getDocumentNames(Pn);

    JsonArray docs = new JsonArray();
    for (int i = 0; i < documentNames.size(); i++) {
      String html = getFileInfo(documentNames.get(i));
      String fileId = extractFileId(html);

      String dateString = extractModifiedDate(html);
      if (dateString == null) {
        continue;
      }
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm:ss a", Locale.ENGLISH);
      LocalDateTime modifiedAt = LocalDateTime.parse(dateString, formatter);

      // Create a Docs object and set its properties
      JsonObject doc = new JsonObject();
      doc.put("file_name", documentNames.get(i));
      doc.put("doc_modified_at", modifiedAt.toInstant(ZoneOffset.UTC).toEpochMilli());
      doc.put("location", fileId); // save id to location

      // Add the document to the list
      docs.add(doc);
    }
    // Return the list of documents
    return docs;
  }

  /**
   * Download documents from DMS system
   *
   * @param fs   FileSystem object for file operations
   * @param docs List of documents to download
   */
  public static void downloadDmsDocs(
      Vertx vertx,
      JsonArray docs,
      String toFolder,
      int toSubFolderDeep,
      int toSubFolderLen) {

    docs.forEach(item -> {
      FileSystem fs = vertx.fileSystem();
      JsonObject doc = (JsonObject) item;
      String fileName = doc.getString("file_name");

      DocsService docsService = new DocsService();
      docsService.searchDocsByName(JsonObject.of("file_name", fileName))
          .onSuccess(list -> {
            if (list.isEmpty()) {
              String tempFile = fs.createTempFileBlocking(null, null);
              log.debug("[Dms][DOWNLOAD] {} from Dms server to {}...", fileName, tempFile);

              CompletableFuture.runAsync(() -> {
                byte[] bytes = getDocumentBytes(doc.getString("location"));
                if (bytes.length > 0 && bytes.length != 581) {
                  log.info("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, bytes.length);

                  var f2 = fs.writeFile(tempFile, Buffer.buffer(bytes));
                  f2.onSuccess(v -> {
                    FSUtils.updateFileModifiedDate(tempFile, doc.getLong("doc_modified_at"));
                    FSUtils.setFileInfo(
                        fs,
                        tempFile,
                        fileName,
                        toFolder,
                        toSubFolderDeep,
                        toSubFolderLen,
                        "MOVE");
                  });
                } else {
                  log.error("[Dms][DOWNLOAD] {} from Dms server, size {}", fileName, bytes.length);
                }
              });

            }
          });

    });

  }
}
