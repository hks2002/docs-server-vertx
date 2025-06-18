/**********************************************************************************************************************
 * @Author                : <>                                                                                        *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : <>                                                                                        *
 * @LastEditDate          : 2025-05-20 13:41:05                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestFuture {

  public static Future<String> f1(Boolean b) {
    if (!b) {
      return Future.failedFuture("f1 Failed");
    }
    return Future.succeededFuture("f1 Success");
  }

  public static Future<String> f2(Boolean b) {
    if (!b) {
      return Future.failedFuture("f2 Failed");
    }
    return Future.succeededFuture("f2 Success");
  }

  @Test
  public void testFuture1() {
    f1(true).onSuccess(a -> {
      log.info("Success: {}", a);
    }).onFailure(a -> {
      log.info("Failed: {}", a);
    }).onComplete(a -> {
      log.info("Complete: {}", a);
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    });

  }

  @Test
  public void testFuture2() {
    f1(false).onSuccess(a -> {
      log.info("Success: {}", a);
    }).onFailure(a -> {
      log.info("Failed: {}", a.getMessage());
    }).onComplete(a -> {
      log.info("Complete: {}", a);
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    });

  }

  @Test
  public void testFuture3() {
    f1(true).onSuccess(a -> {
      log.info("Success: {}", a);
    }).onFailure(a -> {
      log.info("Failed: {}", a);
    }).onComplete(a -> {
      log.info("Complete: {}", a);
    }).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true);
    }).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true);
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    });

  }

  @Test
  public void testFuture4() {
    f1(false).onSuccess(a -> {
      log.info("Success: {}", a);
    }).onFailure(a -> {
      log.info("Failed: {}", a.getMessage());
    }).onComplete(a -> {
      log.info("Complete: {}", a);
    }).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true);
    }).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true);
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    }).onFailure(a -> {
      log.info("Failed: {}", a.getMessage());
    });

  }

  @Test
  public void testFuture5() {
    f1(false).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true);
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    });

  }

  @Test
  public void testFuture6() {
    f1(true).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true);
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    });

  }

}
