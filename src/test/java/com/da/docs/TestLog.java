/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-10 20:02:13                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.db.DB;
import com.da.docs.utils.ConfigUtils;
import com.da.docs.utils.FSUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mysqlclient.MySQLConnectOptions;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestLog {

  @Test
  public void testLog1(Vertx vertx, VertxTestContext testContext) throws Throwable {
    JsonObject mysqlConfig = ConfigUtils.getConfig("config-dev.json").getJsonObject("mysql");
    JsonObject mssqlConfig = ConfigUtils.getConfig("config-dev.json").getJsonObject("mssql");
    MySQLConnectOptions mysqlOptions = new MySQLConnectOptions(mysqlConfig);
    MSSQLConnectOptions mssqlOptions = new MSSQLConnectOptions(mssqlConfig);

    DB.initDB(mysqlOptions, mssqlOptions);

    String sqlTemplate = """
                INSERT INTO
          log (
            template_id,
            v0,
            v1,
            v2,
            v3,
            v4,
            v5,
            v6,
            v7,
            v8,
            v9
          )
        VALUES
          (
            '#{template_id}',
            '#{v0}',
            '#{v1}',
            '#{v2}',
            '#{v3}',
            '#{v4}',
            '#{v5}',
            '#{v6}',
            '#{v7}',
            '#{v8}',
            '#{v9}'
          )
                """;
    JsonObject log1 = new JsonObject();
    log1.put("template_id", 204);
    log1.put("v0", "v0");
    log1.put("v1", "v1");
    log1.put("v2", "v2");
    log1.put("v3", "v3");
    log1.put("v4", "v4");
    log1.put("v5", "v5");
    log1.put("v6", "v6");
    log1.put("v7", "v7");
    log1.put("v8", "v8");
    log1.put("v9", "v9");

    DB.insertBySql(sqlTemplate, log1, 0)
        .onFailure(e -> {
          log.error(e.getMessage());
        }).onComplete(ar -> {
          testContext.completeNow();
        });
  }

  @Test
  public void testLog2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    JsonObject mysqlConfig = ConfigUtils.getConfig("config-dev.json").getJsonObject("mysql");
    JsonObject mssqlConfig = ConfigUtils.getConfig("config-dev.json").getJsonObject("mssql");
    MySQLConnectOptions mysqlOptions = new MySQLConnectOptions(mysqlConfig);
    MSSQLConnectOptions mssqlOptions = new MSSQLConnectOptions(mssqlConfig);

    DB.initDB(mysqlOptions, mssqlOptions);
    FSUtils.setup("c:/docs", 2, 3, vertx);

    JsonObject log = new JsonObject();
    log.put("template_id", 204);
    log.put("v0", "v0");
    log.put("v1", "v1");
    log.put("v2", "v2");
    log.put("v3", "v3");
    log.put("v4", "v4");
    log.put("v5", "v5");
    log.put("v6", "v6");
    log.put("v7", "v7");
    log.put("v8", "v8");
    log.put("v9", "v9");
    log.put("log_at", LocalDateTime.now());

    DB.insertByFile("insertLog", log, 0).onComplete(ar -> {
      testContext.completeNow();
    });
  }

}
