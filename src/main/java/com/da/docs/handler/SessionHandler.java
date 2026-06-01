/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-16 11:51:49                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-25 11:43:18                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import com.da.docs.WebRouter;
import com.da.docs.annotation.AllMapping;
import com.da.docs.annotation.Permission;
import com.da.docs.serviceStatic.RESPONSE;

import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorizations;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

@Log4j2
@AllMapping
public class SessionHandler implements Handler<RoutingContext> {

  private static final String[] PUBLIC_PATHS = {
      "/docs-api/server-info",
      "/docs-api/login",
      "/docs-api/logout"
  };

  @Override
  public void handle(RoutingContext context) {
    String requestPath = context.normalizedPath();

    User u = context.user();

    if (isPublicPath(requestPath)) {
      context.next();
      return;
    }

    if (u == null) {
      RESPONSE.unauthorized(context, "Session expired or not logged in");
      return;
    }

    log.trace("user: {} cached in session", u.principal());

    if (!checkPermission(context, u, requestPath)) {
      return;
    }

    context.next();
  }

  private boolean isPublicPath(String path) {
    for (String publicPath : PUBLIC_PATHS) {
      if (publicPath.equals(path)) {
        return true;
      }
    }
    return false;
  }

  private boolean checkPermission(RoutingContext context, User user, String requestPath) {
    Class<?> handlerClass = WebRouter.getHandlerClassByPath(requestPath);
    if (handlerClass == null) {
      return true;
    }

    Permission permissionAnnotation = handlerClass.getAnnotation(Permission.class);
    if (permissionAnnotation == null) {
      return true;
    }

    String requiredPermission = permissionAnnotation.value();
    Authorizations authorizations = user.authorizations();
    if (!authorizations.verify(PermissionBasedAuthorization.create(requiredPermission))) {
      log.trace("Permission check failed for user: {}, required permission: {}, path: {}",
          user.principal(), requiredPermission, requestPath);
      RESPONSE.forbidden(context);
      return false;
    }

    log.trace("Permission check passed for user: {}, permission: {}, path: {}",
        user.principal(), requiredPermission, requestPath);
    return true;
  }

}