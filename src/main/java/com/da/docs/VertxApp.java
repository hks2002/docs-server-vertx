/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-27 16:01:51                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-05-24 15:31:39                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxApp {
  public static void main(String[] args) {
    // Set Json Object mapper Global
    DatabindCodec.mapper().registerModule(new JavaTimeModule())
        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    if (args.length == 0) {
      log.info("No args, use default Verticle: com.da.docs.WebServerVerticle");
      args = new String[] { "com.da.docs.WebServerVerticle" };
    }

    VertxApplicationHooks hooks = new VertxAppHooks();
    VertxApplication app = new VertxApplication(args, hooks);
    app.launch();

  }
}
