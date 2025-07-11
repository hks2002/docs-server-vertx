/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-10 20:02:13                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;

import com.da.docs.db.DB;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestSQL {

  @Test
  public void testSQL() throws Throwable {
    log.info(DB.replacePlaceholder("'#{A}'", JsonObject.of("A", "AAA'AA'A")));
    log.info(DB.replacePlaceholder("'#{A}'", JsonObject.of("A", "AAA\"AA\"A")));
  }

}
