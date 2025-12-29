/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-12-25 16:37:55                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.utils.ResultSetUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestDB {

  @Test
  public void testDB(Vertx vertx, VertxTestContext testContext) throws Throwable {
    VertxHolder.init(vertx);
    JsonObject mysqlConfig = VertxHolder.appConfig.getJsonObject("mysql");

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(mysqlConfig.getString("jdbcUrl"));
    config.setUsername(mysqlConfig.getString("user"));
    config.setPassword(mysqlConfig.getString("password"));
    config.setMinimumIdle(2);

    HikariDataSource ds = new HikariDataSource(config);
    try {
      Connection conn = ds.getConnection();
      String sql = "SELECT * from docs LIMIT 10";
      Statement stmt = conn.createStatement();
      var rs = stmt.executeQuery(sql);
      List<JsonObject> list = ResultSetUtils.toList(rs);
      log.info("{}", list.toString());

      conn.close();
      ds.close();
    } catch (Exception e) {
      log.error("{}", e.getMessage());
    }
  }

}
