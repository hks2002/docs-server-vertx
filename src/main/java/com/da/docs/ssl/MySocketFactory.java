/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-06-03 10:07:32                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-06-03 10:29:35                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.da.docs.utils.SSLContextUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class MySocketFactory extends SSLSocketFactory {

  private static SSLSocketFactory socketFactory;

  public static SocketFactory getDefault() {
    socketFactory = SSLContextUtils.getMySSLContext().getSocketFactory();
    return socketFactory;
  }

  @Override
  public Socket createSocket(Socket arg0, String arg1, int arg2, boolean arg3) throws IOException {
    return null;
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return socketFactory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return socketFactory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
    return socketFactory.createSocket(arg0, arg1);
  }

  @Override
  public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
    return socketFactory.createSocket(arg0, arg1);
  }

  @Override
  public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3)
      throws IOException, UnknownHostException {
    return socketFactory.createSocket(arg0, arg1, arg2, arg3);
  }

  @Override
  public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
    return socketFactory.createSocket(arg0, arg1, arg2, arg3);
  }
}