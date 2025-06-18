/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-05-18 11:58:41                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.util.ArrayList;
import java.util.List;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.SqlResult;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FuncService {

  public Future<SqlResult<Void>> addFunc(JsonObject obj) {
    return DB.insertByFile("insertFunc", obj);
  }

  public Future<SqlResult<Void>> modifyFunc(JsonObject obj) {
    return DB.updateByFile("updateFunc", obj);
  }

  public Future<List<JsonObject>> searchFunc() {
    return DB.queryByFile("queryFunc", null).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

  public Future<List<JsonObject>> searchFuncByCode(JsonObject obj) {
    return DB.queryByFile("queryFuncByCode", obj).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

}
