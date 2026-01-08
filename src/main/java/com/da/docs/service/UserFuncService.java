/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-19 10:45:24                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.util.List;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UserFuncService {

  public Future<Integer> addUserFunc(JsonObject obj) {
    return DB.insertByFile("insertUserFunc", obj);
  }

  public Future<Integer> addUserFunc(String login_name, String func_code, boolean enable) {
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

  public Future<Integer> modifyUserFunc(JsonObject obj) {
    return DB.updateByFile("updateUserFunc", obj);
  }

  public Future<Integer> modifyUserFunc(String login_name, String func_code, boolean enable) {
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

      int user_id = d1.get(0).getNumber("id").intValue();
      int func_id = d2.get(0).getNumber("id").intValue();

      return searchUserFuncByUserIdFuncId(JsonObject.of(
          "user_id", user_id,
          "func_id", func_id)).compose(list -> {
            if (list.isEmpty()) {
              return addUserFunc(JsonObject.of(
                  "user_id", user_id,
                  "func_id", func_id,
                  "enable", enable));
            } else {
              return modifyUserFunc(JsonObject.of(
                  "user_id", user_id,
                  "func_id", func_id,
                  "enable", enable));
            }
          });
    });

  }

  public Future<List<JsonObject>> searchUserFuncByLoginNameFuncCode(JsonObject userFunc) {
    return DB.queryByFile("queryUserFuncByLoginNameFuncCode", userFunc);
  }

  public Future<List<JsonObject>> searchUserFuncByUserIdFuncId(JsonObject userFunc) {
    return DB.queryByFile("queryUserFuncByUserIdFuncId", userFunc);
  }
}
