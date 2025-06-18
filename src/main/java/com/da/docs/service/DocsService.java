/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-04-02 16:48:46                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-06 16:05:36                                                                       *
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
public class DocsService {

  public Future<SqlResult<Void>> addDocs(JsonObject obj) {
    return DB.insertByFile("insertDocs", obj);
  }

  public Future<SqlResult<Void>> modifyDocs(JsonObject obj) {
    return DB.updateByFile("updateDocs", obj);
  }

  public Future<SqlResult<Void>> removeDocs(JsonObject obj) {
    return DB.updateByFile("deleteDocs", obj);
  }

  public Future<SqlResult<Void>> removeDuplicateDocsByName() {
    return DB.updateByFile("deleteDuplicateDocsByName", new JsonObject());
  }

  public Future<List<JsonObject>> searchDocs(JsonObject obj) {
    return DB.queryByFile("queryDocs", obj).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

  public Future<List<JsonObject>> searchDocsByName(JsonObject obj) {
    return DB.queryByFile("queryDocsByName", obj).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

  public Future<List<JsonObject>> searchDocsFromTLSOLDByName(JsonObject obj) {
    return DB.queryByFile("queryDocsFromTLSOLDByName", obj, 1).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

  public Future<List<JsonObject>> searchDocsByMD5(JsonObject obj) {
    return DB.queryByFile("queryDocsByMD5", obj).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

}
