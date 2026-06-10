/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-06-10 14:08:43                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.serviceStatic.DB;
import com.da.docs.utils.ConfigUtils;

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
    ConfigUtils.setUpConfig(vertx, "config-dev.json").andThen((config) -> {
      DB.setup(vertx);

      testContext.completeNow();
    });
  }

  @Test
  public void testDB1(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      log.info(config);
      DB.setup(vertx);

      // A simple query
      DB.queryBySql("SELECT * from docs LIMIT 10", new JsonObject())
          .onSuccess(result -> {
            log.info("Success: {}", result);
          })
          .onComplete(ar -> {
            testContext.completeNow();
          });

      String sql = """
          INSERT INTO
              docs (
                file_name,
                dms_id,
                size,
                is_link,
                doc_create_at,
                doc_modified_at,
                md5,
                create_at,
                create_by
              )
            VALUES
              (
                '9C2323G01_A.pdf',
                '518767',
                '366174',
                'false',
                '2026-06-10 13:34:30',
                '2026-06-10 13:34:30',
                'dea446717c8c286be3dbf78016bacb19',
                '2026-06-10T13:34:30.476054289',
                '0'
              );
          """;

      // get the error message
      DB.insertBySql(sql, new JsonObject())
          .onSuccess(result -> {
            log.info("Success: {}", result);
          }).onFailure(e -> {
            log.error("Error: {}\n {}", e.getMessage(), e.getCause());
          })
          .onComplete(ar -> {
            testContext.completeNow();
          });
    });

  }

  @Test
  public void testDB2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {

      log.info(config);
      DB.setup(vertx);

      // A simple query
      DB.queryBySql("SELECT TOP 10 * FROM SORDER", new JsonObject(), 1)
          .onSuccess(result -> {
            log.info("Success: {}", result);
          })
          .onComplete(ar -> {
            testContext.completeNow();
          });
    });
  }
}