/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-06-23 16:42:59                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.handler;

import java.util.List;
import java.util.Optional;

import com.da.docs.annotation.PostMapping;
import com.da.docs.service.LogService;
import com.da.docs.utils.CommonUtils;
import com.da.docs.utils.FSUtils;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.impl.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorizations;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.log4j.Log4j2;

/**
 * Changed the SendDirectoryListing and SendFile methods.
 * <p>
 * And other necessary changes, class name, log.
 */
@Log4j2
@PostMapping("/docs-api/upload")
public class UploadHandler implements Handler<RoutingContext> {
  private String docsRoot = "/mnt/docs";
  private int folderDeep = 0;
  private int folderLen = 3;

  public UploadHandler(JsonObject config) {
    JsonObject docsConfig = Utils.isWindows()
        ? config.getJsonObject("docs").getJsonObject("windows")
        : config.getJsonObject("docs").getJsonObject("linux");
    this.docsRoot = docsConfig.getString("docsRoot", docsRoot);
    var uploadConfig = Optional.ofNullable(config.getJsonObject("upload")).orElse(new JsonObject());
    this.folderDeep = uploadConfig.getInteger("folderDeep", folderDeep);
    this.folderLen = uploadConfig.getInteger("folderLen", folderLen);
  }

  @Override
  public void handle(RoutingContext context) {
    User user = Optional.ofNullable(context.user()).orElse(User.create(new JsonObject()));

    Authorizations authorizations = user.authorizations();
    if (!authorizations.verify(PermissionBasedAuthorization.create("DOCS_WRITE"))) {
      log.trace("{},{}", user.principal(), authorizations);
      Response.forbidden(context);
      return;
    }

    HttpServerRequest request = context.request();
    String referer = request.headers().get(HttpHeaders.REFERER);
    String refererPath = CommonUtils.normalizePath(referer);

    if (folderDeep == 0 && refererPath == null) {
      Response.badRequest(context, "Bad Request, referer is null");
      return;
    }

    Vertx vertx = context.vertx();
    LogService logService = new LogService();

    String ip = CommonUtils.getTrueRemoteIp(request);
    String loginName = user.principal().getString("login_name", "");
    String fullName = user.principal().getString("full_name", "");

    List<FileUpload> uploads = context.fileUploads();
    if (uploads.size() > 1) {
      Response.badRequest(context, "Bad Request, only one file is allowed");
      return;
    }

    FileUpload upload = uploads.get(0);
    log.trace("{} {} {} {}", upload.uploadedFileName(), upload.fileName(), upload.contentType(), upload.size());
    String fromPath = upload.uploadedFileName().replace('\\', '/');
    String finalFolder = folderDeep == 0 ? docsRoot + refererPath : docsRoot;

    String lastModified = context.request().getHeader("Last-Modified");
    FSUtils.updateFileModifiedDate(fromPath, lastModified);

    FSUtils
        .setFileInfo(
            vertx.fileSystem(),
            fromPath,
            upload.fileName(),
            finalFolder,
            folderDeep,
            folderLen,
            "MOVE")
        .onSuccess(rst -> {
          log.info("Upload file {} success", upload.fileName());
          logService.addLog("DOC_UPLOAD_SUCCESS", ip, loginName, fullName, upload.fileName());
          Response.success(context, "Upload file success");
        })
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          logService.addLog("DOC_UPLOAD_FAILED", ip, loginName, fullName,
              upload.fileName());
          Response.internalError(context, "Upload file failed");
        });
  }

}
