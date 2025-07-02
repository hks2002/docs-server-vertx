/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-07-02 12:05:02                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.util.ArrayList;
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

  public Future<Object> addUser(JsonObject obj) {
    return DB.insertByFile("insertUser", obj);
  }

  public Future<Object> modifyUser(JsonObject obj) {
    return DB.updateByFile("updateUser", obj);
  }

  public Future<List<JsonObject>> searchUser() {
    return DB.queryByFile("queryUser", null).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

  public Future<List<JsonObject>> searchUserByLoginName(JsonObject obj) {
    return DB.queryByFile("queryUserByLoginName", obj).compose(rowSet -> {
      List<JsonObject> list = new ArrayList<>();
      rowSet.forEach(row -> {
        list.add(row);
      });
      return Future.succeededFuture(list);
    });
  }

  public Future<User> login(String adServerUrl, String adServerDomain, String searchBase,
      UsernamePasswordCredentials credentials, String ip) {
    String userName = credentials.getUsername();
    String password = credentials.getPassword();

    LogService logService = new LogService();
    ADServices adServices = new ADServices();
    UserService userService = new UserService();
    UserFuncService userFuncService = new UserFuncService();

    return adServices.adAuthorization(adServerUrl, adServerDomain, searchBase, userName, password)
        .onFailure(err -> {
          logService.addLog("LOGIN_FAILED", ip, userName);
        })
        .compose(userAuthorized -> {
          String fullName = userAuthorized.getString("full_name");
          logService.addLog("LOGIN_SUCCESS", ip, userName, fullName);

          return userService.searchUserByLoginName(userAuthorized)
              .compose(userSearched -> {
                User user = User.create(userAuthorized);

                if (userSearched.size() == 0) {
                  return userService.addUser(userAuthorized)
                      .onFailure(err -> {
                        logService.addLog("USER_INIT_FAILED", ip, userName, fullName);
                      })
                      .compose(ar -> {
                        logService.addLog("USER_INIT_SUCCESS", ip, userName, fullName);

                        // init user's access
                        Future<Object> f11 = userFuncService.addUserFunc(userName, "DOCS_READ", true);
                        Future<Object> f12 = userFuncService.addUserFunc(userName, "DOCS_WRITE", false);

                        return Future.all(f11, f12)
                            .onFailure(err -> {
                              logService.addLog("DOC_ACCESS_INIT_FAILED", ip, userName, fullName);
                            })
                            .compose(ar2 -> {
                              logService.addLog("DOC_ACCESS_INIT_SUCCESS", ip, userName, fullName);
                              user.authorizations().put("docs", PermissionBasedAuthorization.create("DOCS_READ"));
                              return Future.succeededFuture(user);
                            });
                      });

                } else if (userSearched.size() == 1) {
                  // check if user has read permission
                  Future<List<JsonObject>> f1 = userFuncService
                      .searchUserFuncByLoginNameFuncCode(
                          JsonObject.of("login_name", userName, "func_code", "DOCS_READ"));
                  Future<List<JsonObject>> f2 = userFuncService
                      .searchUserFuncByLoginNameFuncCode(
                          JsonObject.of("login_name", userName, "func_code", "DOCS_WRITE"));

                  return Future.all(f1, f2)
                      .onFailure(err -> {
                        logService.addLog("DOC_ACCESS_SET_FAILED", ip, userName, fullName);
                      })
                      .compose(data -> {
                        logService.addLog("DOC_ACCESS_SET_SUCCESS", ip, userName, fullName);
                        List<JsonObject> d1 = data.resultAt(0);
                        List<JsonObject> d2 = data.resultAt(1);
                        Set<Authorization> authorizations = new HashSet<>();

                        if (d1.size() > 0) {
                          authorizations.add(PermissionBasedAuthorization.create("DOCS_READ"));
                        }
                        if (d2.size() > 0) {
                          authorizations.add(PermissionBasedAuthorization.create("DOCS_WRITE"));
                        }

                        user.authorizations().put("docs", authorizations);
                        return Future.succeededFuture(user);
                      });

                } else {
                  // should never happened, login name is unique by database
                  return Future.failedFuture("User name is not unique");
                }
              });
        });
  }

}
