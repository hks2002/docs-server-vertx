/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-05-18 11:59:12                                                                       *
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
public class UserFuncService {

  public Future<SqlResult<Void>> addUserFunc(JsonObject obj) {
    return DB.insertByFile("insertUserFunc", obj);
  }

  public Future<SqlResult<Void>> addUserFunc(String login_name, String func_code, boolean enable) {

    UserService userService = new UserService();
    FuncService funcService = new FuncService();

    Future<List<JsonObject>> f1 = userService.searchUserByLoginName(JsonObject.of("login_name", login_name));
    Future<List<JsonObject>> f2 = funcService.searchFuncByCode(JsonObject.of("func_code", func_code));

    return Future.all(f1, f2).compose(data -> {
      List<JsonObject> d1 = data.resultAt(0);
      List<JsonObject> d2 = data.resultAt(1);

      if (d1.isEmpty() || d2.isEmpty()) {
        return Future.failedFuture("user or func not found");
      }

      return addUserFunc(JsonObject.of(
          "user_id", d1.get(0).getNumber("id"),
          "func_id", d2.get(0).getNumber("id"),
          "enable", enable));
    });

  }

  public Future<List<JsonObject>> searchUserFuncByLoginNameFuncCode(JsonObject userFunc) {
    return DB.queryByFile("queryUserFuncByLoginNameFuncCode", userFunc).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

}
