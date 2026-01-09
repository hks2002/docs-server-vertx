/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-20 11:15:15                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 17:03:23                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.db;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.da.docs.VertxApp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import lombok.extern.log4j.Log4j2;

/**
 * Important: if connection cannot be established,
 * edit these files:
 * - /etc/crypto-policies/back-ends/java.config
 * - JAVA_HOME/lib/security/java.security
 */
@Log4j2
public class DB {
  private static Pool[] pools = new Pool[2];

  public static void initDB() {
    MySQLConnectOptions mysqlOptions = new MySQLConnectOptions(VertxApp.appConfig.getJsonObject("mysql"));
    MSSQLConnectOptions mssqlOptions = new MSSQLConnectOptions(VertxApp.appConfig.getJsonObject("mssql"));
    ClientSSLOptions sslOptions = new ClientSSLOptions().setTrustAll(true);
    mysqlOptions.setSslOptions(sslOptions);
    mssqlOptions.setSslOptions(sslOptions);

    PoolOptions mysqlPoolOptions = new PoolOptions(VertxApp.appConfig.getJsonObject("mysqlPoolOptions"));
    PoolOptions mssqlPoolOptions = new PoolOptions(VertxApp.appConfig.getJsonObject("mssqlPoolOptions"));

    Pool mysqlClient = Pool.pool(VertxApp.vertx, mysqlOptions, mysqlPoolOptions);
    Pool mssqlClient = Pool.pool(VertxApp.vertx, mssqlOptions, mssqlPoolOptions);

    DB.pools[0] = mysqlClient;
    DB.pools[1] = mssqlClient;
  }

  public static Future<Boolean> validate(String sqlTemplate, JsonObject json) {
    Pattern pattern = Pattern.compile("#\\{([^}]*)\\}");
    Matcher matcher = pattern.matcher(sqlTemplate);

    Set<String> requiredFields = new HashSet<>();
    while (matcher.find()) {
      // matcher.group(0) => #{name} matcher.group(1) => name
      requiredFields.add(matcher.group(1));
    }

    Boolean b = requiredFields.stream().allMatch(json.fieldNames()::contains);
    if (!b) {
      var msg = "UnMatching SQL Placeholder and Json Parameters";
      log.error("{}:\n{}\n{}", msg, sqlTemplate, json.encodePrettily());
      return Future.failedFuture(msg);
    }
    return Future.succeededFuture(b);
  }

  public static String replacePlaceholder(String sqlTemplate, JsonObject json) {
    String newSql = sqlTemplate;
    for (String key : json.fieldNames()) {
      String value = json.getString(key);
      if (value != null) {
        String escapeVal = value.replace("'", "\\\\'").replace("\"", "\\\\\"");
        newSql = newSql.replaceAll("'#\\{" + key + "\\}'", "'" + escapeVal + "'"); // replace string values
        newSql = newSql.replaceAll("#\\{" + key + "\\}", escapeVal); // replace non-string values
      } else {
        newSql = newSql.replaceAll("'#\\{" + key + "\\}'", "NULL"); // replace string values
        newSql = newSql.replaceAll("#\\{" + key + "\\}", "NULL"); // replace non-string values
      }
    }
    return newSql;
  }

  public static Future<List<JsonObject>> queryBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    // deep copy to avoid modify the original json
    JsonObject limitJson = json.copy();
    if (!json.containsKey("offset")) {
      limitJson.put("offset", 0);
    }
    if (!json.containsKey("limit")) {
      limitJson.put("limit", 100);
    }

    return validate(sqlTemplate, limitJson).compose(valid -> {
      String sql = replacePlaceholder(sqlTemplate, limitJson);
      return pools[dbIdx].query(sql)
          .execute()
          .compose(rowSet -> {
            List<JsonObject> list = new ArrayList<>();
            for (Row row : rowSet) {
              list.add(row.toJson());
            }

            return Future.succeededFuture(list);
          })
          .onFailure(e -> {
            log.error("{}\n\n{}\n\n{}\n", e.getMessage(), sql, limitJson.encodePrettily());
          });
    });
  }

  public static Future<List<JsonObject>> queryBySql(String sqlTemplate, JsonObject json) {
    return queryBySql(sqlTemplate, json, 0);
  }

  public static Future<List<JsonObject>> queryByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return VertxApp.fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
        })
        .compose(sqlTemplate -> {
          return queryBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<List<JsonObject>> queryByFile(String sqlFileName, JsonObject json) {
    return queryByFile(sqlFileName, json, 0);
  }

  public static Future<Integer> insertBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    // deep copy to avoid modify the original json
    JsonObject updateJson = json.copy();
    updateJson.put("create_at", LocalDateTime.now());
    updateJson.put("create_by", 0);

    return validate(sqlTemplate, updateJson).compose(valid -> {
      String sql = replacePlaceholder(sqlTemplate, updateJson);
      return pools[dbIdx].query(sql)
          .execute()
          .compose(rowSet -> {
            Integer affected = rowSet.rowCount();
            if (affected > 0) {
              return Future.succeededFuture(affected);
            } else {
              return Future.failedFuture("Insert failed: no rows affected");
            }
          })
          .onFailure(e -> {
            log.error("{}\n\n{}\n\n{}\n", e.getMessage(), sql, updateJson.encodePrettily());
          });
    });
  }

  public static Future<Integer> insertBySql(String sqlTemplate, JsonObject json) {
    return insertBySql(sqlTemplate, json, 0);
  }

  public static Future<Integer> insertByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return VertxApp.fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
        })
        .compose(sqlTemplate -> {
          return insertBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<Integer> insertByFile(String sqlFileName, JsonObject json) {
    return insertByFile(sqlFileName, json, 0);
  }

  public static Future<Integer> updateBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    // deep copy to avoid modify the original json
    JsonObject updateJson = json.copy();
    updateJson.put("update_at", LocalDateTime.now());
    updateJson.put("update_by", 0);

    return validate(sqlTemplate, updateJson).compose(valid -> {
      String sql = replacePlaceholder(sqlTemplate, updateJson);
      return pools[dbIdx].query(sql)
          .execute()
          .compose(rowSet -> {
            Integer affected = rowSet.rowCount();
            if (affected > 0) {
              return Future.succeededFuture(affected);
            } else {
              return Future.failedFuture("Update failed: no rows affected");
            }
          })
          .onFailure(e -> {
            log.error("{}\n\n{}\n\n{}\n", e.getMessage(), sql, updateJson.encodePrettily());
          });
    });
  }

  public static Future<Integer> updateBySql(String sqlTemplate, JsonObject json) {
    return updateBySql(sqlTemplate, json, 0);
  }

  public static Future<Integer> updateByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return VertxApp.fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
        })
        .compose(sqlTemplate -> {
          return updateBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<Integer> updateByFile(String sqlFileName, JsonObject json) {
    return updateByFile(sqlFileName, json, 0);
  }

  public static Future<Integer> deleteBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    return validate(sqlTemplate, json).compose(valid -> {
      String sql = replacePlaceholder(sqlTemplate, json);
      return pools[dbIdx].query(sql)
          .execute()
          .compose(rowSet -> {
            Integer affected = rowSet.rowCount();
            if (affected > 0) {
              return Future.succeededFuture(affected);
            } else {
              return Future.failedFuture("Delete failed: no rows affected");
            }
          })
          .onFailure(e -> {
            log.error("{}\n\n{}\n\n{}\n", e.getMessage(), sql, json.encodePrettily());
          });
    });

  }

  public static Future<Integer> deleteBySql(String sqlTemplate, JsonObject json) {
    return deleteBySql(sqlTemplate, json, 0);
  }

  public static Future<Integer> deleteByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return VertxApp.fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
        })
        .compose(sqlTemplate -> {
          return deleteBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<Integer> deleteByFile(String sqlFileName, JsonObject json) {
    return deleteByFile(sqlFileName, json, 0);
  }

}
