/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-05-19 16:54:08                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-12-25 16:07:37                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs;

import io.vertx.core.Vertx;
import io.vertx.core.VertxBuilder;
import io.vertx.core.VertxOptions;
import io.vertx.launcher.application.HookContext;
import io.vertx.launcher.application.VertxApplicationHooks;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VertxAppHooks implements VertxApplicationHooks {

  @Override
  public VertxBuilder createVertxBuilder(VertxOptions options) {
    log.info("VertxOptions:\n{}", options.toJson().encodePrettily());
    VertxHolder.vertxOptions = options;
    return Vertx.builder().with(options);
  }

  @Override
  public void beforeDeployingVerticle(HookContext context) {
    VertxHolder.init(context.vertx());
    context.deploymentOptions().setConfig(VertxHolder.appConfig);
  }

  @Override
  public void afterVerticleDeployed(HookContext context) {
    // context.vertx().setPeriodic(30000, id -> {
    // context.deploymentOptions().setConfig(VertxHolder.appConfig);
    // log.debug("Config updated:\n{}", VertxHolder.appConfig.encodePrettily());
    // });
  }

  @Override
  public void afterFailureToStartVertx(HookContext context, Throwable t) {
    log.error("{}", t.getMessage());
  }

  @Override
  public void afterFailureToDeployVerticle(HookContext context, Throwable t) {
    log.error("{}", t.getMessage());
    context.vertx().close();
  }

}
