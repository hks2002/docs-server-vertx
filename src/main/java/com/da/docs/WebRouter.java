/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-28 00:21:32                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-09-23 22:00:02                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.da.docs.annotation.AllMapping;
import com.da.docs.annotation.DeleteMapping;
import com.da.docs.annotation.GetMapping;
import com.da.docs.annotation.PostMapping;
import com.da.docs.utils.PackageUtils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.impl.SessionHandlerImpl;
import io.vertx.ext.web.impl.RouterImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WebRouter extends RouterImpl {

  class Pair {
    // Return a map entry (key-value pair) from the specified values
    public static <T, U> Map.Entry<T, U> of(T first, U second) {
      return new AbstractMap.SimpleEntry<>(first, second);
    }
  }

  private List<Class<?>> getAllPathHandler(String packageName) {
    List<Class<?>> routeHandler = new ArrayList<>();

    PackageUtils.getClassesInJarPackage(packageName).forEach(handlerName -> {
      try {
        var handlerClass = Class.forName(handlerName);

        var routeMappingAnnotation = handlerClass.getAnnotation(AllMapping.class);
        if (routeMappingAnnotation != null) {
          routeHandler.add(handlerClass);
        }
      } catch (Exception e) {
        log.error("{}", e);
      }
    });

    return routeHandler;
  }

  private SortedMap<String, Map.Entry<HttpMethod, Class<?>>> getPathHandler(String packageName) {
    SortedMap<String, Map.Entry<HttpMethod, Class<?>>> pathsHandler = new TreeMap<>();

    PackageUtils.getClassesInJarPackage(packageName).forEach(handlerName -> {
      try {
        var handlerClass = Class.forName(handlerName);

        var getMappingAnnotation = handlerClass.getAnnotation(GetMapping.class);
        if (getMappingAnnotation != null) {
          pathsHandler.put(getMappingAnnotation.value(), Pair.of(HttpMethod.GET, handlerClass));
        }
        var postMappingAnnotation = handlerClass.getAnnotation(PostMapping.class);
        if (postMappingAnnotation != null) {
          pathsHandler.put(postMappingAnnotation.value(), Pair.of(HttpMethod.POST, handlerClass));
        }
        var deleteMappingAnnotation = handlerClass.getAnnotation(DeleteMapping.class);
        if (deleteMappingAnnotation != null) {
          pathsHandler.put(deleteMappingAnnotation.value(), Pair.of(HttpMethod.DELETE, handlerClass));
        }
      } catch (Exception e) {
        log.error("{}", e);
      }
    });

    return pathsHandler;
  }

  private Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
    try {
      return clazz.getDeclaredConstructor(parameterTypes);
    } catch (Exception e) {
      return null;
    }
  }

  private Object getHandlerInstance(Class<?> clazz) {
    Constructor<?> constructor = null;
    try {
      constructor = getConstructor(clazz);
      return constructor.newInstance();
    } catch (Exception e) {
      log.error("{}", e);
      return null;
    }
  }

  private void registerGlobalHandler() {
    var uploadConfig = VertxHolder.appConfig.getJsonObject("upload");
    BodyHandler bodyHandler = BodyHandler.create();
    bodyHandler.setUploadsDirectory(VertxHolder.fs.createTempDirectoryBlocking(""));
    bodyHandler.setDeleteUploadedFilesOnEnd(false);
    bodyHandler.setBodyLimit(uploadConfig.getInteger("bodyLimit", -1));

    var sessionConfig = VertxHolder.appConfig.getJsonObject("session");
    SessionHandlerImpl sessionHandler = new SessionHandlerImpl(LocalSessionStore.create(VertxHolder.vertx));
    sessionHandler.setSessionTimeout(sessionConfig.getLong("sessionTimeout", SessionHandler.DEFAULT_SESSION_TIMEOUT));
    sessionHandler.setCookieHttpOnlyFlag(sessionConfig.getBoolean("cookieHttpOnly", true));
    sessionHandler.setCookieSecureFlag(sessionConfig.getBoolean("cookieSecure", true));

    this.route().handler(bodyHandler);
    this.route().handler(sessionHandler);
    // this.route().handler(FaviconHandler.create(vertx));
    // this.route().handler(StaticHandler.create());
  }

  @SuppressWarnings("unchecked")
  private void registerAllPathHandler() {
    var routeHandler = getAllPathHandler("com.da.docs.handler");

    routeHandler.forEach(handlerClass -> {
      log.info("{}", handlerClass);

      Object handlerInstance = getHandlerInstance(handlerClass);
      if (Handler.class.isAssignableFrom(handlerClass)) {
        super.route().handler((Handler<RoutingContext>) handlerInstance);
      } else {
        log.error("Handler Class {} is not supported", handlerClass.getName());
      }
    });
  }

  @SuppressWarnings("unchecked")
  private void registerPathHandler() {
    var pathsHandler = getPathHandler("com.da.docs.handler");

    pathsHandler.forEach((path, handler) -> {
      log.info("{} {} {}", handler.getKey(), path, handler.getValue());
      HttpMethod method = handler.getKey();
      Class<?> handlerClass = handler.getValue();
      Object handlerInstance = getHandlerInstance(handlerClass);

      if (Handler.class.isAssignableFrom(handlerClass)) {
        super.route(method, path).handler((Handler<RoutingContext>) handlerInstance);
      } else if (FaviconHandler.class.isAssignableFrom(handlerClass)) {
        super.route(method, path).handler((FaviconHandler) handlerInstance);
      } else if (StaticHandler.class.isAssignableFrom(handlerClass)) {
        super.route(method, path).handler((StaticHandler) handlerInstance);
      } else {
        log.error("Handler Class {} is not supported", handlerClass.getName());
      }

    });

  }

  public WebRouter(Vertx vertx) {
    super(vertx);
    registerGlobalHandler();
    registerAllPathHandler();
    registerPathHandler();
  }

}
