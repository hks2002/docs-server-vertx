/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-04 15:18:03                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


package com.da.docs.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.authorization.Authorization;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class UserService {
  private final ADServices adServices = new ADServices();
  private final UserFuncService userFuncService = new UserFuncService();

  public Future<Object> addUser(JsonObject obj) {
    return DB.insertByFile("insertUser", obj);
  }

  public Future<User> addUser(User user) {
    JsonObject userInfo = user.principal();
    String userName = userInfo.getString("login_name");
    String fullName = userInfo.getString("full_name");

    return addUser(userInfo)
        .onFailure(err -> {
          LogService.addLog("USER_INIT_FAILED", null, userName, fullName);
        })
        .compose(ar -> {
          LogService.addLog("USER_INIT_SUCCESS", null, userName, fullName);

          // init user's access
          Future<Object> f11 = userFuncService.addUserFunc(userName, "DOCS_READ", false);
          Future<Object> f12 = userFuncService.addUserFunc(userName, "DOCS_WRITE", false);

          return Future.all(f11, f12)
              .onFailure(err -> {
                LogService.addLog("DOC_ACCESS_INIT_FAILED", null, userName, fullName);
              })
              .compose(ar2 -> {
                LogService.addLog("DOC_ACCESS_INIT_SUCCESS", null, userName, fullName);
                return Future.succeededFuture(user);
              });
        });
  }

  public Future<Object> modifyUser(JsonObject obj) {
    return DB.updateByFile("updateUser", obj);
  }

  public Future<List<JsonObject>> searchUser() {
    return DB.queryByFile("queryUser", null);
  }

  public Future<User> searchUser(User user) {
    JsonObject userInfo = user.principal();
    String userName = userInfo.getString("login_name");
    String fullName = userInfo.getString("full_name");

    // check if user has read/write permission
    Future<List<JsonObject>> f1 = userFuncService
        .searchUserFuncByLoginNameFuncCode(
            JsonObject.of("login_name", userName, "func_code", "DOCS_READ"));
    Future<List<JsonObject>> f2 = userFuncService
        .searchUserFuncByLoginNameFuncCode(
            JsonObject.of("login_name", userName, "func_code", "DOCS_WRITE"));
    Future<List<JsonObject>> f3 = userFuncService
        .searchUserFuncByLoginNameFuncCode(
            JsonObject.of("login_name", userName, "func_code", "ADMIN"));

    return Future.all(f1, f2, f3)
        .onFailure(err -> {
          LogService.addLog("DOC_ACCESS_SET_FAILED", null, userName, fullName);
        })
        .compose(data -> {
          LogService.addLog("DOC_ACCESS_SET_SUCCESS", null, userName, fullName);
          List<JsonObject> d1 = data.resultAt(0);
          List<JsonObject> d2 = data.resultAt(1);
          List<JsonObject> d3 = data.resultAt(2);
          Set<Authorization> authorizations = new HashSet<>();

          if (d1.size() > 0) {
            authorizations.add(PermissionBasedAuthorization.create("DOCS_READ"));
          }
          if (d2.size() > 0) {
            authorizations.add(PermissionBasedAuthorization.create("DOCS_WRITE"));
          }
          if (d3.size() > 0) {
            authorizations.add(PermissionBasedAuthorization.create("ADMIN"));
          }

          user.authorizations().put("docs", authorizations);
          return Future.succeededFuture(user);
        });
  }

  public Future<List<JsonObject>> searchUserByLoginName(JsonObject obj) {
    return DB.queryByFile("queryUserByLoginName", obj);
  }

  public Future<User> login(UsernamePasswordCredentials credentials, String ip) {
    String userName = credentials.getUsername();
    String password = credentials.getPassword();

    return adServices.adAuthorization(userName, password)
        .onFailure(err -> {
          LogService.addLog("LOGIN_FAILED", ip, userName);
        })
        .compose(useInfo -> {
          String fullName = useInfo.getString("full_name");
          LogService.addLog("LOGIN_SUCCESS", ip, userName, fullName);

          User user = User.create(useInfo);

          return searchUserByLoginName(useInfo)
              .compose(userSearched -> {
                if (userSearched.size() == 0) {
                  return addUser(user);
                } else if (userSearched.size() == 1) {
                  return searchUser(user);
                } else {
                  // should never happened, login name is unique by database
                  return Future.failedFuture("User name is not unique");
                }
              });
        });
  }

}
