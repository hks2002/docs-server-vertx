/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-08-06 11:43:23                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-10-03 17:41:15                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TrustAllSSLSocketFactory extends SSLSocketFactory {
  private SSLSocketFactory factory;

  public TrustAllSSLSocketFactory() {
    log.debug("TrustAllSSLSocketFactory initialized, all SSL certificates will be trusted.");
    try {
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new TrustManager[] { new X509TrustManager() {
        public void checkClientTrusted(X509Certificate[] xcs, String string) {
        }

        public void checkServerTrusted(X509Certificate[] xcs, String string) {
        }

        public X509Certificate[] getAcceptedIssuers() {
          return null;
        }
      } }, new SecureRandom());
      factory = ctx.getSocketFactory();
    } catch (Exception ex) {
      log.error("Error initializing TrustAllSSLSocketFactory: {}", ex.getMessage());
      throw new RuntimeException(ex);
    }
  }

  public static SocketFactory getDefault() {
    return new TrustAllSSLSocketFactory();
  }

  @Override
  public String[] getDefaultCipherSuites() {
    return factory.getDefaultCipherSuites();
  }

  @Override
  public String[] getSupportedCipherSuites() {
    return factory.getSupportedCipherSuites();
  }

  @Override
  public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
    return factory.createSocket(s, host, port, autoClose);
  }

  @Override
  public Socket createSocket(String host, int port) throws IOException {
    return factory.createSocket(host, port);
  }

  @Override
  public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
    return factory.createSocket(host, port, localHost, localPort);
  }

  @Override
  public Socket createSocket(InetAddress host, int port) throws IOException {
    return factory.createSocket(host, port);
  }

  @Override
  public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
      throws IOException {
    return factory.createSocket(address, port, localAddress, localPort);
  }
}