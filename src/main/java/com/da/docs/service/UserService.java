/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 19:34:13                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.da.docs.VertxApp;
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
  private JsonObject defaultAccess = null;
  private String adminPassword = null;
  private final UserFuncService userFuncService = new UserFuncService();

  public UserService() {
    defaultAccess = VertxApp.appConfig.getJsonObject("defaultAccess");
    adminPassword = VertxApp.appConfig.getString("adminPassword");
  }

  public void setup(JsonObject defaultAccess, String adminPassword) {
    this.defaultAccess = defaultAccess;
    this.adminPassword = adminPassword;
  }

  public Future<Integer> addUser(JsonObject obj) {
    return DB.insertByFile("insertUser", obj);
  }

  public Future<User> addUser(User user, String ip) {
    JsonObject userInfo = user.principal();
    String userName = userInfo.getString("login_name");
    String fullName = userInfo.getString("full_name");

    return addUser(userInfo)
        .onFailure(err -> {
          LogService.addLog("USER_INIT_FAILED", ip, userName);
        })
        .compose(ar -> {
          LogService.addLog("USER_INIT_SUCCESS", ip, userName);
          // init user's access
          Future<Integer> f11 = userFuncService.addUserFunc(userName, "DOCS_READ", defaultAccess.getBoolean("read"));
          Future<Integer> f12 = userFuncService.addUserFunc(userName, "DOCS_WRITE", defaultAccess.getBoolean("write"));

          return Future.all(f11, f12)
              .onFailure(err -> {
                LogService.addLog("DOC_ACCESS_INIT_FAILED", ip, userName);
              })
              .compose(ar2 -> {
                LogService.addLog("DOC_ACCESS_INIT_SUCCESS", ip, userName);
                return Future.succeededFuture(user);
              });
        });
  }

  public Future<Integer> modifyUser(JsonObject obj, String ip) {
    return DB.updateByFile("updateUser", obj).onSuccess(ar -> {
      LogService.addLog("USER_UPDATE_SUCCESS", ip, obj.getString("login_name"));
    }).onFailure(err -> {
      LogService.addLog("USER_UPDATE_FAILED", ip, obj.getString("login_name"));
    });
  }

  public Future<List<JsonObject>> searchUser() {
    return DB.queryByFile("queryUser", null);
  }

  public Future<User> setUserPermission(User user, String ip) {
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
          LogService.addLog("DOC_ACCESS_SET_FAILED", ip, userName);
        })
        .compose(data -> {
          LogService.addLog("DOC_ACCESS_SET_SUCCESS", ip, userName);
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

  /**
   * login and return the user with permission
   */
  public Future<User> login(UsernamePasswordCredentials credentials, String ip) {
    String userName = credentials.getUsername();
    String password = credentials.getPassword();

    if (userName.equals("admin") && password.equals(adminPassword)) {
      // for build in admin
      LogService.addLog("LOGIN_SUCCESS", ip, userName, "Administrator");

      return searchUserByLoginName(JsonObject.of("login_name", "admin"))
          .compose(userSearched -> {
            if (userSearched.size() == 0) {
              return Future.failedFuture("user admin not exists in database");
            } else if (userSearched.size() == 1) {
              JsonObject userInfo = userSearched.get(0);
              // add the missing 'full_name'
              userInfo.put("full_name", userInfo.getString("first_name") + " " + userInfo.getString("last_name"));
              return setUserPermission(User.create(userInfo), ip);
            } else {
              // should never happened, login name is unique by database
              return Future.failedFuture("User name is not unique");
            }
          });
    } else {
      // for ldap users:
      return new ADServices().Authenticate(userName, password)
          .onFailure(err -> {
            LogService.addLog("LOGIN_FAILED", ip, userName);
          })
          .compose(useInfo -> {
            LogService.addLog("LOGIN_SUCCESS", ip, userName);

            User user = User.create(useInfo);

            return searchUserByLoginName(useInfo)
                .compose(userSearched -> {
                  if (userSearched.size() == 0) {
                    return addUser(user, ip)
                        .compose(u -> {
                          return setUserPermission(user, ip);
                        });
                  } else if (userSearched.size() == 1) {
                    if (userSearched.get(0).getString("first_name") == null ||
                        userSearched.get(0).getString("last_name") == null ||
                        userSearched.get(0).getString("email") == null) {
                      useInfo.put("id", userSearched.get(0).getString("id"));
                      modifyUser(useInfo, ip);
                    }
                    return setUserPermission(user, ip);
                  } else {
                    // should never happened, login name is unique by database
                    return Future.failedFuture("User name is not unique");
                  }
                });
          });
    }

  }

}
