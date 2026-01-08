/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-17 15:15:23                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import java.util.Arrays;
import java.util.List;

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

  @Test
  public void testFuture7() {
    f1(false)
        .otherwise(a -> {
          log.info("otherwise: {}", a);
          return "Recovered";
        }).compose(a -> {
          log.info("Compose: {}", a);
          return f2(true);
        });

    f1(true)
        .otherwise(a -> {
          log.info("otherwise: {}", a);
          return "Recovered";
        }).compose(a -> {
          log.info("Compose: {}", a);
          return f2(true);
        });

  }

  @Test
  public void testFuture8() {
    List<Future<String>> futureList = Arrays.asList(Future.succeededFuture("Result 1"),
        Future.succeededFuture("Result 2"));
    Future.all(futureList).onComplete(ar -> {
      if (ar.succeeded()) {
        List<String> results = ar.result().list();
        log.info("Results: {}", results);
        // Handle the list of results here
      } else {
        // Handle failure
      }
    });
  }

  @Test
  public void testFuture9() {
    f1(true).compose(a -> {
      log.info("Compose: {}", a);
      return f2(true).onSuccess(a2 -> {
        log.info("Success: {}", a2);
      });
    }).andThen(a -> {
      log.info("AndThen: {}", a);
    });

  }
}
