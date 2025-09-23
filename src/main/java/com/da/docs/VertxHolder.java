/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-09-18 12:08:15                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-19 11:19:09                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/


package com.da.docs;

import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxHolder {
  static public Vertx vertx = null;
  static public FileSystem fs = null;
}
