/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-26 15:30:06                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.annotation.GetMapping;
import com.da.docs.annotation.Permission;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Handler;
import io.vertx.core.internal.net.RFC3986;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@GetMapping("/docs-api/docs/*")
@Permission("DOCS_READ")
public class DocsBrowseHandler implements Handler<RoutingContext> {

  @Override
  public void handle(RoutingContext context) {
    // decode URL path
    String uriDecodedPath = RFC3986.decodeURIComponent(context.normalizedPath(), true);
    // if the normalized path is null it cannot be resolved
    if (uriDecodedPath == null) {
      RESPONSE.badRequest(context, "Invalid path: " + context.request().path());
      return;
    }
    // will normalize and handle all paths as UNIX paths
    String path = RFC3986.removeDotSegments(uriDecodedPath.replace('\\', '/'));
    RESPONSE.sendStatic(context, path);
  }
}