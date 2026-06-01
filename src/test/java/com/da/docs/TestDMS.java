/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-22 11:29:00                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.pojo.ResultData;
import com.da.docs.serviceStatic.DMS;
import com.da.docs.serviceStatic.HTTP;
import com.da.docs.utils.ConfigUtils;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestDMS {

  @Test
  void test2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    log.info("base64Encode: {}", DMS.au_URLEncode("00870"));
    log.info("base64Encode: {}", DMS.au_URLEncode("UPPER(F.nomfich) LIKE '*RRT130807*'"));
    testContext.completeNow();
  }

  @Test
  void test3(Vertx vertx, VertxTestContext testContext) throws Throwable {
    String xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <resultdata>
          <result>
            <result_name>service_executed</result_name>
            <result_value>true</result_value>
            <result_code>0</result_code>
            <result_detail>3*500*1000|630917|650961|650965</result_detail>
          </result>
        </resultdata>
        """;

    XmlMapper xmlMapper = new XmlMapper();
    ResultData data = xmlMapper.readValue(xml, ResultData.class);
    String resultDetail = data.getResults().get(0).getResultDetail();
    String[] parts = resultDetail.split("\\|");
    List<String> fileIds = new ArrayList<>(Arrays.asList(parts));
    fileIds.remove(0);

    log.info("Parsed XML: {}", data);
    log.info("Result Detail: {}", resultDetail);
    log.info("Result Detail: {}", Arrays.asList(parts));
    log.info("File IDs: {}", fileIds);
    testContext.completeNow();
  }

  @Test
  void test4(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      HTTP.setup(vertx);
      String dmsServer = config.getString("dmsServer");

      DMS.doLogin(dmsServer).onSuccess(sessionId -> {
        log.info("doLogin: {}", sessionId);
        testContext.completeNow();
      });
    });

  }

  @Test
  void test1(Vertx vertx, VertxTestContext testContext) throws Throwable {
    ConfigUtils.setUpConfig(vertx, "config-dev.json").onSuccess((config) -> {
      HTTP.setup(vertx);
      DMS.setup(vertx);

      DMS.getDocuments("RRT130806")
          .onSuccess(result -> {
            log.info("getDocuments: {}", result.encodePrettily());
          })
          .onFailure(e -> {
            log.error("Error: {}", e.getCause());
          })
          .andThen(r -> {
            testContext.completeNow();
          });
    });
  }
}
