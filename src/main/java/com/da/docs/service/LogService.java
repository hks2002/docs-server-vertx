/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-10-04 12:46:54                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.time.LocalDateTime;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LogService {

  public static Future<Integer> addLog(
      String TCode,
      String v0,
      String v1,
      String v2,
      String v3,
      String v4,
      String v5,
      String v6,
      String v7,
      String v8,
      String v9) {

    return DB.queryByFile("queryLogTemplateByCode", JsonObject.of("template_code", TCode))
        .compose(result -> {
          if (result.size() == 0) {
            var msg = "template code " + TCode + " not found";
            log.error(msg);
            return Future.failedFuture(msg);
          } else if (result.size() == 1) {
            JsonObject log = new JsonObject();

            log.put("template_id", result.iterator().next().getInteger("id"));
            log.put("v0", v0);
            log.put("v1", v1);
            log.put("v2", v2);
            log.put("v3", v3);
            log.put("v4", v4);
            log.put("v5", v5);
            log.put("v6", v6);
            log.put("v7", v7);
            log.put("v8", v8);
            log.put("v9", v9);
            log.put("log_at", LocalDateTime.now());

            return DB.insertByFile("insertLog", log);
          } else {
            var msg = "template code " + TCode + " more than one";
            log.error(msg);
            return Future.failedFuture(msg);
          }
        });
  }

  public static Future<Integer> addLog(
      String TCode,
      String v0,
      String v1,
      String v2,
      String v3,
      String v4,
      String v5,
      String v6,
      String v7,
      String v8) {
    return addLog(TCode, v0, v1, v2, v3, v4, v5, v6, v7, v8, null);
  }

  public static Future<Integer> addLog(
      String TCode,
      String v0,
      String v1,
      String v2,
      String v3,
      String v4,
      String v5,
      String v6,
      String v7) {
    return addLog(TCode, v0, v1, v2, v3, v4, v5, v6, v7, null, null);
  }

  public static Future<Integer> addLog(
      String TCode,
      String v0,
      String v1,
      String v2,
      String v3,
      String v4,
      String v5,
      String v6) {
    return addLog(TCode, v0, v1, v2, v3, v4, v5, v6, null, null, null);
  }

  public static Future<Integer> addLog(
      String TCode,
      String v0,
      String v1,
      String v2,
      String v3,
      String v4,
      String v5) {
    return addLog(TCode, v0, v1, v2, v3, v4, v5, null, null, null, null);
  }

  public static Future<Integer> addLog(
      String TCode,
      String v0,
      String v1,
      String v2,
      String v3,
      String v4) {
    return addLog(TCode, v0, v1, v2, v3, v4, null, null, null, null, null);
  }

  public static Future<Integer> addLog(String TCode, String v0, String v1, String v2, String v3) {
    return addLog(TCode, v0, v1, v2, v3, null, null, null, null, null, null);
  }

  public static Future<Integer> addLog(String TCode, String v0, String v1, String v2) {
    return addLog(TCode, v0, v1, v2, null, null, null, null, null, null, null);
  }

  public static Future<Integer> addLog(String TCode, String v0, String v1) {
    return addLog(TCode, v0, v1, null, null, null, null, null, null, null, null);
  }

  public static Future<Integer> addLog(String TCode, String v0) {
    return addLog(TCode, v0, null, null, null, null, null, null, null, null, null);
  }

  public static Future<Integer> addLog(String TCode) {
    return addLog(TCode, null, null, null, null, null, null, null, null, null, null);
  }
}
