/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-05-19 16:54:08                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-24 16:55:40                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs;

import com.da.docs.utils.ConfigUtils;

import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxAppHooks implements VertxApplicationHooks {

  @Override
  public JsonObject afterConfigParsed(JsonObject config) {
    ConfigUtils.logConfig(config);
    return config;
  }

  @Override
  public VertxBuilder createVertxBuilder(VertxOptions options) {
    return Vertx.builder().with(options);
  }

  @Override
  public void afterVertxStarted(HookContext context) {
  }

  @Override
  public void afterFailureToStartVertx(HookContext context, Throwable t) {
    log.error("{}", t.getCause());
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
  }

  @Override
  public void afterVerticleDeployed(HookContext context) {
    ConfigUtils.retrieveConfig(context);
  }

  @Override
  public void afterFailureToDeployVerticle(HookContext context, Throwable t) {
    log.error("{}", t.getCause());
    context.vertx().close();
  }

}