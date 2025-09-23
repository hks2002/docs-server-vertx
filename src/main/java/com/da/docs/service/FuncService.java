/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-19 09:43:25                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs.service;

import java.util.List;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FuncService {

  public Future<Object> addFunc(JsonObject obj) {
    return DB.insertByFile("insertFunc", obj);
  }

  public Future<Object> modifyFunc(JsonObject obj) {
    return DB.updateByFile("updateFunc", obj);
  }

  public Future<List<JsonObject>> searchFunc() {
    return DB.queryByFile("queryFunc", null);
  }

  public Future<List<JsonObject>> searchFuncByCode(JsonObject obj) {
    return DB.queryByFile("queryFuncByCode", obj);
  }

}
