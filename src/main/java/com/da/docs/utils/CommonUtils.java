/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-09 23:29:08                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-05-22 19:43:53                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.MIMEHeader;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.ParsableMIMEValue;
import io.vertx.ext.web.impl.Utils;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class CommonUtils {

  public static String getTrueRemoteIp(HttpServerRequest request) {
    String ip = request.getHeader("X-Forwarded-For");

    if (ip == null || ip.length() == 0) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.length() == 0) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.length() == 0) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.length() == 0) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.length() == 0) {
      ip = request.connection().remoteAddress(true).hostAddress();
    }
    return ip;
  }

  public static String normalizePath(String path) {
    try {
      URI uri = new URI(path);
      return uri.normalize().getPath();
    } catch (URISyntaxException e) {
      return null;
    }
  }

  private static final Collection<MIMEHeader> LISTING_ACCEPT = Arrays.asList(
      new ParsableMIMEValue("text/html").forceParse(),
      new ParsableMIMEValue("text/plain").forceParse(),
      new ParsableMIMEValue("application/json").forceParse());

  public static String getAccept(RoutingContext context) {
    var headerValues = context.parsedHeaders();
    final List<MIMEHeader> accepts = headerValues.accept();
    String accept = "text/plain";

    if (accepts != null) {
      MIMEHeader header = headerValues
          .findBestUserAcceptedIn(accepts, LISTING_ACCEPT);
      if (header != null) {
        accept = header.component() + "/" + header.subComponent();
      }
    }
    return accept;
  }

  public static boolean nameMatch(String name, Set<String> listSearch) {
    for (String elementA : listSearch) {
      if (name.contains(elementA)) {
        return true; // 找到第一个匹配的元素就返回
      }
    }
    return false;
  }

  /**
   * Pads a string with a specified character on the right side to ensure it has
   *
   * @param s
   * @param length
   * @param c
   * @return
   */
  public static String withRightPad(String s, int length, char c) {
    StringBuilder sb = new StringBuilder(s);
    while (sb.length() < length) {
      sb.append(c);
    }
    return sb.toString();
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
  public static String getPathByFileName(
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
    String subFolders = withRightPad(cleanName, toSubFolderDeep * toSubFolderLen, '0');
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

  public static UsernamePasswordCredentials getCredentials(String authorization) {
    if (authorization == null) {
      return null;
    }
    int idx = authorization.indexOf(' ');
    if (idx == -1) {
      return null;
    }
    String userName;
    String password;
    String decoded = new String(Utils.base64Decode(authorization.substring(idx + 1)), StandardCharsets.UTF_8);
    int colonIdx = decoded.indexOf(":");
    if (colonIdx != -1) {
      userName = decoded.substring(0, colonIdx);
      password = decoded.substring(colonIdx + 1);
    } else {
      userName = decoded;
      password = null;
    }
    return new UsernamePasswordCredentials(userName, password);
  }

}
