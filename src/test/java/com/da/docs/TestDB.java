/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 18:52:06                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.utils.ConfigUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.mssqlclient.MSSQLBuilder;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestDB {

  @Test
  public void testDB(Vertx vertx, VertxTestContext testContext) throws Throwable {
    JsonObject config = ConfigUtils.getConfig("config-dev.json").getJsonObject("mysql");
    log.info(config);
    MySQLConnectOptions options = new MySQLConnectOptions(config);
    PoolOptions poolOptions = new PoolOptions().setMaxSize(1);

    Pool client = MySQLBuilder.pool()
        .with(poolOptions)
        .connectingTo(options)
        .build();

    // A simple query
    client
        .query("SELECT * from docs LIMIT 10")
        .execute()
        .onSuccess(result -> {
          RowSet<Row> rowSet = result;
          for (Row row : rowSet) {
            System.out.println(row.toJson());
          }
        })
        .onComplete(ar -> {

          // Now close the pool
          client.close();

          testContext.completeNow();
        });

  }

  @Test
  public void testDB2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    JsonObject config = ConfigUtils.getConfig("config-dev.json").getJsonObject("mssql");
    log.info(config);

    MSSQLConnectOptions options = new MSSQLConnectOptions(config)
        .setSslOptions(new ClientSSLOptions().setTrustAll(true));
    PoolOptions poolOptions = new PoolOptions().setMaxSize(1);

    Pool client = MSSQLBuilder.pool()
        .with(poolOptions)
        .connectingTo(options)
        .build();

    // A simple query
    client
        .query("SELECT TOP 10 * FROM SORDER")
        .execute()
        .onSuccess(result -> {
          RowSet<Row> rowSet = result;
          for (Row row : rowSet) {
            System.out.println(row.toJson());
          }
        })
        .onComplete(ar -> {

          // Now close the pool
          client.close();

          testContext.completeNow();
        });

  }

}