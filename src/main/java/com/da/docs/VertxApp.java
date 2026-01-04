/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-27 16:01:51                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 20:00:41                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.launcher.application.VertxApplication;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxApp {
  static public String[] appArgs = null;
  static public JsonObject appConfig = null;
  static public Vertx vertx = null;
  static public FileSystem fs = null;

  public static void main(String[] args) {
    // Set Json Object mapper Global
    DatabindCodec.mapper().registerModule(new JavaTimeModule())
        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    appArgs = args;
    log.info("Starting VertxApplication with args: {}", Arrays.toString(args));

    VertxApplicationHooks hooks = new VertxAppHooks();
    VertxApplication app = new VertxApplication(args, hooks);
    app.launch();

  }
}
