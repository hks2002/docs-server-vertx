/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-20 11:15:15                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-17 00:31:04                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.db;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;
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
  private static FileSystem fs;

  public static void initDB(Vertx vertx) {
    JsonObject mysqlConfig = vertx.getOrCreateContext().config().getJsonObject("mysql");
    JsonObject mssqlConfig = vertx.getOrCreateContext().config().getJsonObject("mssql");

    HikariConfig mysqlHikariConfig = new HikariConfig();
    mysqlHikariConfig.setJdbcUrl(mysqlConfig.getString("jdbcUrl", "jdbc:mysql://localhost:3306\\docs"));
    mysqlHikariConfig.setUsername(mysqlConfig.getString("user", "docs"));
    mysqlHikariConfig.setPassword(mysqlConfig.getString("password", "<PASSWORD>"));
    HikariDataSource mysqlDS = new HikariDataSource(mysqlHikariConfig);

    HikariConfig mssqlHikariConfig = new HikariConfig();
    mssqlHikariConfig.setJdbcUrl(mssqlConfig.getString("jdbcUrl", "jdbc:sqlserver://localhost:1433;databaseName=docs"));
    mssqlHikariConfig.setUsername(mssqlConfig.getString("user", "docs"));
    mssqlHikariConfig.setPassword(mssqlConfig.getString("password", "<PASSWORD>"));
    mssqlHikariConfig.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    HikariDataSource mssqlDS = new HikariDataSource(mssqlHikariConfig);

    // DB Pool options， minSize allWays be 1
    PoolOptions mysqlPoolOptions = new PoolOptions(mysqlConfig.getJsonObject("poolOptions", new JsonObject()));
    PoolOptions mssqlPoolOptions = new PoolOptions(mssqlConfig.getJsonObject("poolOptions", new JsonObject()));

    // DB Connection Pool
    Pool mysqlPool = JDBCPool.pool(vertx, mysqlDS, mysqlPoolOptions);
    Pool mssqlPool = JDBCPool.pool(vertx, mssqlDS, mssqlPoolOptions);

    DB.pools[0] = mysqlPool;
    DB.pools[1] = mssqlPool;
    DB.fs = vertx.fileSystem();
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

  public static String replacePlaceholder(Buffer sqlTemplate) {
    return sqlTemplate.toString().replaceAll("'#\\{", "#{").replaceAll("}'", "}");
  }

  public static Future<RowSet<JsonObject>> queryBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    if (!json.containsKey("offset")) {
      json.put("offset", 0);
    }
    if (!json.containsKey("limit")) {
      json.put("limit", 100);
    }

    return validate(sqlTemplate, json).compose(valid -> {
      return SqlTemplate
          .forQuery(pools[dbIdx], sqlTemplate)
          .mapTo(Row::toJson)
          .mapFrom(TupleMapper.jsonObject())
          .execute(json)
          .onSuccess(ar -> {
            log.trace("{}\n{}\n", sqlTemplate, json.encodePrettily());
          })
          .onFailure(ar -> {
            log.error("{}\n{}\n{}\n", ar.getMessage(), sqlTemplate, json.encodePrettily());
          });
    });
  }

  public static Future<RowSet<JsonObject>> queryBySql(String sqlTemplate, JsonObject json) {
    return queryBySql(sqlTemplate, json, 0);
  }

  public static Future<RowSet<JsonObject>> queryByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return queryBySql(replacePlaceholder(sqlTemplate), json, dbIdx);
        });
  }

  public static Future<RowSet<JsonObject>> queryByFile(String sqlFileName, JsonObject json) {
    return queryByFile(sqlFileName, json, 0);
  }

  public static Future<SqlResult<Void>> insertBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    json.put("create_at", LocalDateTime.now());
    json.put("create_by", 0);
    json.put("update_at", LocalDateTime.now());
    json.put("update_by", 0);

    return validate(sqlTemplate, json).compose(valid -> {
      return SqlTemplate
          .forUpdate(pools[dbIdx], sqlTemplate)
          .mapFrom(TupleMapper.jsonObject())
          .execute(json)
          .onSuccess(ar -> {
            log.trace("{}\n{}\n", sqlTemplate, json.encodePrettily());
          })
          .onFailure(ar -> {
            log.error("{}\n{}\n{}\n", ar.getMessage(), sqlTemplate, json.encodePrettily());
          });
    });

  }

  public static Future<SqlResult<Void>> insertBySql(String sqlTemplate, JsonObject json) {
    return insertBySql(sqlTemplate, json, 0);
  }

  public static Future<SqlResult<Void>> insertByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return insertBySql(replacePlaceholder(sqlTemplate), json, dbIdx);
        });
  }

  public static Future<SqlResult<Void>> insertByFile(String sqlFileName, JsonObject json) {
    return insertByFile(sqlFileName, json, 0);
  }

  public static Future<SqlResult<Void>> updateBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    json.put("update_at", LocalDateTime.now());
    json.put("update_by", 0);

    return validate(sqlTemplate, json).compose(valid -> {
      return SqlTemplate
          .forUpdate(pools[dbIdx], sqlTemplate)
          .mapFrom(TupleMapper.jsonObject())
          .execute(json)
          .onSuccess(ar -> {
            log.trace("{}\n{}\n", sqlTemplate, json.encodePrettily());
          })
          .onFailure(ar -> {
            log.error("{}\n{}\n{}\n", ar.getMessage(), sqlTemplate, json.encodePrettily());
          });
    });

  }

  public static Future<SqlResult<Void>> updateBySql(String sqlTemplate, JsonObject json) {
    return updateBySql(sqlTemplate, json, 0);
  }

  public static Future<SqlResult<Void>> updateByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return updateBySql(replacePlaceholder(sqlTemplate), json, dbIdx);
        });
  }

  public static Future<SqlResult<Void>> updateByFile(String sqlFileName, JsonObject json) {
    return updateByFile(sqlFileName, json, 0);
  }

  public static Future<SqlResult<Void>> deleteBySql(String sqlTemplate, JsonObject json, int dbIdx) {
    return validate(sqlTemplate, json).compose(valid -> {
      return SqlTemplate
          .forUpdate(pools[dbIdx], sqlTemplate)
          .mapFrom(TupleMapper.jsonObject())
          .execute(json)
          .onSuccess(ar -> {
            log.trace("{}\n{}\n", sqlTemplate, json.encodePrettily());
          })
          .onFailure(ar -> {
            log.error("{}\n{}\n{}\n", ar.getMessage(), sqlTemplate, json.encodePrettily());
          });
    });

  }

  public static Future<SqlResult<Void>> deleteBySql(String sqlTemplate, JsonObject json) {
    return deleteBySql(sqlTemplate, json, 0);
  }

  public static Future<SqlResult<Void>> deleteByFile(String sqlFileName, JsonObject json, int dbIdx) {
    return fs.readFile("sqlTemplate/" + sqlFileName + ".sql")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
        })
        .compose(sqlTemplate -> {
          return deleteBySql(replacePlaceholder(sqlTemplate), json, dbIdx);
        });
  }

  public static Future<SqlResult<Void>> deleteByFile(String sqlFileName, JsonObject json) {
    return deleteByFile(sqlFileName, json, 0);
  }

}
