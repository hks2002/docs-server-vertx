/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-27 16:01:51                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-21 22:27:23                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.launcher.application.VertxApplication;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxApp {
  static public String[] appArgs = null;

  public static void main(String[] args) {
    // Set Json Object mapper Global
    DatabindCodec.mapper().registerModule(new JavaTimeModule())
        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    appArgs = args;
    log.info("Starting VertxApplication with args: {}", Arrays.toString(args));

    VertxApplication app = new VertxApplication(args, new VertxAppHooks());
    app.launch();
  }
}
