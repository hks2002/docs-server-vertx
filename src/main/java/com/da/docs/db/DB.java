/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-20 11:15:15                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-07-11 09:17:30                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.da.docs.utils.ResultSetUtils;
import com.zaxxer.hikari.HikariDataSource;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

/**
 * Important: if connection cannot be established,
 * edit these files:
 * - /etc/crypto-policies/back-ends/java.config
 * - JAVA_HOME/lib/security/java.security
 */
@Log4j2
public class DB {
  private static HikariDataSource[] pools = new HikariDataSource[2];
  private static FileSystem fs;

  public static void initDB(Vertx vertx) {
    JsonObject mysqlConfig = vertx.getOrCreateContext().config().getJsonObject("mysql");
    JsonObject mssqlConfig = vertx.getOrCreateContext().config().getJsonObject("mssql");

    JsonObject mysqlPoolOptions = mysqlConfig.getJsonObject("poolOptions", new JsonObject());
    JsonObject mssqlPoolOptions = mssqlConfig.getJsonObject("poolOptions", new JsonObject());

    @SuppressWarnings("resource")
    HikariDataSource mysqlDS = new HikariDataSource();
    mysqlDS.setJdbcUrl(mysqlConfig.getString("jdbcUrl", "jdbc:mysql://localhost:3306/docs"));
    mysqlDS.setUsername(mysqlConfig.getString("user", "docs"));
    mysqlDS.setPassword(mysqlConfig.getString("password", "<PASSWORD>"));
    if (mysqlPoolOptions.containsKey("minimumIdle")) {
      mysqlDS.setMinimumIdle(mysqlPoolOptions.getInteger("minimumIdle"));
    }
    if (mysqlPoolOptions.containsKey("maximumPoolSize")) {
      mysqlDS.setMaximumPoolSize(mysqlPoolOptions.getInteger("maximumPoolSize"));
    }
    if (mysqlPoolOptions.containsKey("idleTimeout")) {
      mysqlDS.setIdleTimeout(mysqlPoolOptions.getLong("idleTimeout"));
    }
    if (mysqlPoolOptions.containsKey("maxLifetime")) {
      mysqlDS.setMaxLifetime(mysqlPoolOptions.getLong("maxLifetime"));
    }

    @SuppressWarnings("resource")
    HikariDataSource mssqlDS = new HikariDataSource();
    mssqlDS.setJdbcUrl(mssqlConfig.getString("jdbcUrl", "jdbc:sqlserver://localhost:3306/docs"));
    mssqlDS.setUsername(mssqlConfig.getString("user", "docs"));
    mssqlDS.setPassword(mssqlConfig.getString("password", "<PASSWORD>"));
    if (mssqlPoolOptions.containsKey("minimumIdle")) {
      mssqlDS.setMinimumIdle(mssqlPoolOptions.getInteger("minimumIdle"));
    }
    if (mssqlPoolOptions.containsKey("maximumPoolSize")) {
      mssqlDS.setMaximumPoolSize(mssqlPoolOptions.getInteger("maximumPoolSize"));
    }
    if (mssqlPoolOptions.containsKey("idleTimeout")) {
      mssqlDS.setIdleTimeout(mssqlPoolOptions.getLong("idleTimeout"));
    }
    if (mssqlPoolOptions.containsKey("maxLifetime")) {
      mssqlDS.setMaxLifetime(mssqlPoolOptions.getLong("maxLifetime"));
    }

    DB.pools[0] = mysqlDS;
    DB.pools[1] = mssqlDS;
    DB.fs = vertx.fileSystem();
  }

  public static void closeAll() {
    for (HikariDataSource ds : pools) {
      if (ds != null && !ds.isClosed()) {
        ds.close();
      }
    }
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
    if (!json.containsKey("offset")) {
      json.put("offset", 0);
    }
    if (!json.containsKey("limit")) {
      json.put("limit", 100);
    }

    return validate(sqlTemplate, json).compose(valid -> {
      Connection conn = null;
      try {
        conn = pools[dbIdx].getConnection();
        String sql = replacePlaceholder(sqlTemplate, json);
        Statement stmt = conn.createStatement();
        var rs = stmt.executeQuery(sql);
        List<JsonObject> list = ResultSetUtils.toList(rs);

        log.trace("{}\n\n{}\n", sqlTemplate, list.toString());
        conn.close();
        return Future.succeededFuture(list);

      } catch (Exception e) {
        log.error("{}\n\n{}\n\n{}\n", e.getMessage(), sqlTemplate, json.encodePrettily());
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e1) {
            log.error("Failed to close connection: {}", e1.getMessage());
          }
        }
        return Future.failedFuture("Run SQL error");
      }

    });
  }

  public static Future<List<JsonObject>> queryBySql(String sqlTemplate, JsonObject json) {
    return queryBySql(sqlTemplate, json, 0);
  }

  public static Future<List<JsonObject>> queryByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return queryBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<List<JsonObject>> queryByFile(String sqlFileName, JsonObject json) {
    return queryByFile(sqlFileName, json, 0);
  }

  public static Future<Object> insertBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    json.put("create_at", LocalDateTime.now());
    json.put("create_by", 0);
    json.put("update_at", LocalDateTime.now());
    json.put("update_by", 0);

    return validate(sqlTemplate, json).compose(valid -> {
      Connection conn = null;
      try {
        conn = pools[dbIdx].getConnection();
        String sql = replacePlaceholder(sqlTemplate, json);
        Statement stmt = conn.createStatement();
        int rs = stmt.executeUpdate(sql);
        conn.close();

        log.trace("{}\n\n{}\n", sqlTemplate, rs);
        if (rs == 0) {
          return Future.failedFuture("Insert failed, no rows affected");
        }
        return Future.succeededFuture(rs);
      } catch (Exception e) {
        log.error("{}\n{}\n{}\n", e.getMessage(), sqlTemplate, json.encodePrettily());
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e1) {
            log.error("Failed to close connection: {}", e1.getMessage());
          }
        }
        return Future.failedFuture("Run SQL error");
      }
    });

  }

  public static Future<Object> insertBySql(String sqlTemplate, JsonObject json) {
    return insertBySql(sqlTemplate, json, 0);
  }

  public static Future<Object> insertByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return insertBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<Object> insertByFile(String sqlFileName, JsonObject json) {
    return insertByFile(sqlFileName, json, 0);
  }

  public static Future<Object> updateBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    json.put("update_at", LocalDateTime.now());
    json.put("update_by", 0);

    return validate(sqlTemplate, json).compose(valid -> {
      Connection conn = null;
      try {
        conn = pools[dbIdx].getConnection();
        String sql = replacePlaceholder(sqlTemplate, json);
        Statement stmt = conn.createStatement();
        int rs = stmt.executeUpdate(sql);
        conn.close();

        log.trace("{}\n\n{}\n", sqlTemplate, rs);
        return Future.succeededFuture(rs);
      } catch (Exception e) {
        log.error("{}\n{}\n{}\n", e.getMessage(), sqlTemplate, json.encodePrettily());
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e1) {
            log.error("Failed to close connection: {}", e1.getMessage());
          }
        }
        return Future.failedFuture("Run SQL error");
      }
    });

  }

  public static Future<Object> updateBySql(String sqlTemplate, JsonObject json) {
    return updateBySql(sqlTemplate, json, 0);
  }

  public static Future<Object> updateByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return updateBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<Object> updateByFile(String sqlFileName, JsonObject json) {
    return updateByFile(sqlFileName, json, 0);
  }

  public static Future<Object> deleteBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    return validate(sqlTemplate, json).compose(valid -> {
      Connection conn = null;
      try {
        conn = pools[dbIdx].getConnection();
        String sql = replacePlaceholder(sqlTemplate, json);
        Statement stmt = conn.createStatement();
        int rs = stmt.executeUpdate(sql);
        conn.close();

        log.trace("{}\n\n{}\n", sqlTemplate, rs);
        if (rs == 0) {
          return Future.failedFuture("Delete failed, no rows affected");
        }
        return Future.succeededFuture(rs);
      } catch (Exception e) {
        log.error("{}\n{}\n{}\n", e.getMessage(), sqlTemplate, json.encodePrettily());
        if (conn != null) {
          try {
            conn.close();
          } catch (SQLException e1) {
            log.error("Failed to close connection: {}", e1.getMessage());
          }
        }
        return Future.failedFuture("Run SQL error");
      }
    });

  }

  public static Future<Object> deleteBySql(String sqlTemplate, JsonObject json) {
    return deleteBySql(sqlTemplate, json, 0);
  }

  public static Future<Object> deleteByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return deleteBySql(sqlTemplate.toString(), json, dbIdx);
        });
  }

  public static Future<Object> deleteByFile(String sqlFileName, JsonObject json) {
    return deleteByFile(sqlFileName, json, 0);
  }

}
