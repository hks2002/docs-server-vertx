/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 19:57:24                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.pojo.ResultData;
import com.da.docs.service.DMSServices;
import com.da.docs.utils.ConfigUtils;
import com.da.docs.utils.FSUtils;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestDMS {

  @Test
  void test1(Vertx vertx, VertxTestContext testContext) throws Throwable {

    String dmsServer = ConfigUtils.getConfig("config-dev.json").getString("dmsServer");
    String dmsServerFast = ConfigUtils.getConfig("config-dev.json").getString("dmsServerFast");

    FSUtils.setup("c:/docs", 3, 3, vertx.fileSystem());
    DMSServices.setup(dmsServer, dmsServerFast, vertx.fileSystem());

    DMSServices.getDocuments("RRT130806")
        .onSuccess(result -> {
          log.info("getDocuments: {}", result.encodePrettily());
        })
        .onFailure(e -> {
          log.error("Error: ", e);
        })
        .andThen(r -> {
          testContext.completeNow();
        });

  }

  @Test
  void test2(Vertx vertx, VertxTestContext testContext) throws Throwable {
    log.info("base64Encode: {}", DMSServices.au_URLEncode("UPPER(F.nomfich) LIKE '*RRT130807*'"));
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
    String dmsServer = ConfigUtils.getConfig("config-dev.json").getString("dmsServer");

    DMSServices.doLogin(dmsServer);

    testContext.completeNow();

  }
}
