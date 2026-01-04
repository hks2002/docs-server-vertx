/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2026-01-04 17:50:32                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2026-01-04 17:50:42                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ConfigUtils {

  public static JsonObject getConfig(String configPath) {
    try (InputStream in = ConfigUtils.class.getClassLoader()
        .getResourceAsStream(configPath)) {
      if (in != null) {
        return new JsonObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
      } else {
        log.warn("Config file not found!");
      }
    } catch (Exception e) {
      log.error("Failed to load config!", e);
    }
    return null;
  }

}
