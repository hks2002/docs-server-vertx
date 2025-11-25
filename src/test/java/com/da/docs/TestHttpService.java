/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-03 13:17:42                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.da.docs.service.HttpService;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@ExtendWith(VertxExtension.class)
public class TestHttpService {

  @Test
  public void test1(Vertx vertx, VertxTestContext testContext) throws Throwable {
    HttpService.get(
        "http://192.168.10.64:4040/cocoon/View/LoginCAD/fr/AW_AutoLogin.html?userName=TEMP&dsn=dmsDS&Client_Type=25&computerName=AWS&LDAPControl=true")
        .onComplete(ar -> {
          if (ar.succeeded()) {
            log.info("Result: " + ar.result());
          } else {
            log.error("Error: ", ar.cause());
          }
        }).andThen(r -> {
          HttpService.get(
              "http://192.168.10.64:4040/cocoon/View/ExecuteService/fr/AW_AuplResult3.html?ServiceName=aws.au&ServiceSubPackage=aws&UserName=TEMP&dsn=dmsDS&Client_Type=25&ServiceParameters=GET_AUTOCOMPLETION@JTI1ODU2QTEwMDE=@&AUSessionID=b19ibgcdj25d0")
              .onComplete(ar -> {
                if (ar.succeeded()) {
                  log.info("Result: " + ar.result());
                  testContext.completeNow();
                } else {
                  log.error("Error: ", ar.cause());
                  testContext.failNow(ar.cause());
                }
              });
        });

  }
}
