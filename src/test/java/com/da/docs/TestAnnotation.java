/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-05-25 00:23:09                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs;

import org.junit.jupiter.api.Test;

import com.da.docs.annotation.PostMapping;
import com.da.docs.handler.LoginHandler;
import com.da.docs.utils.PackageUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestAnnotation {
  @Test
  public void testScanAnnotation() {
    log.info(PackageUtils.getClassesInJarPackage("com.da.docs.handler").toString());
  }

  @Test
  public void testAnnotation() {
    var a = LoginHandler.class.getAnnotation(PostMapping.class);
    log.info(a.value());
  }

}
