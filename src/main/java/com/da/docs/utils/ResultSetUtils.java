/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-07-01 23:17:14                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-02 12:45:22                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.utils;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ResultSetUtils {

  public static List<JsonObject> toList(ResultSet rs) {
    List<JsonObject> list = new ArrayList<>();

    try {
      while (rs.next()) {
        JsonObject json = new JsonObject();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
          String columnName = rs.getMetaData().getColumnName(i);
          Object value = rs.getObject(i);
          if (value instanceof LocalDateTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            value = ((LocalDateTime) value).format(formatter);
          }
          json.put(columnName, value);
        }
        list.add(json);
      }

    } catch (Exception e) {
      log.error("{}", e.getMessage());
    }
    return list;
  }
}
