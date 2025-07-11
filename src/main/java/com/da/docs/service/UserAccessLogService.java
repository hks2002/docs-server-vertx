/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-07-10 20:58:44                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.util.ArrayList;
import java.util.List;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UserAccessLogService {
  public Future<List<JsonObject>> searchUserAccessLogByLoginName(JsonObject obj) {
    return DB.queryByFile("queryUserDocAccessLogByLoginName", obj).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

}
