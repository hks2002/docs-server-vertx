/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-06-03 09:59:30                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-06-03 10:20:24                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.utils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.da.docs.ssl.MyTrustManager;

public class SSLContextUtils {
  public static SSLContext getMySSLContext() {
    try {
      TrustManager[] trustManagers = { new MyTrustManager() };
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustManagers, new java.security.SecureRandom());
      return sc;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create SSLContext", e);
    }
  }
}
