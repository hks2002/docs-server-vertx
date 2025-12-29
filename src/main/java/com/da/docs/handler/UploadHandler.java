/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-12-25 18:44:49                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs.handler;

import java.util.List;
import java.util.Optional;

import com.da.docs.annotation.PostMapping;
import com.da.docs.service.DocsService;
import com.da.docs.service.LogService;
import com.da.docs.utils.CommonUtils;
import com.da.docs.utils.FSUtils;
import com.da.docs.utils.Response;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
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

  public UploadHandler() {
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

    if (refererPath == null) {
      Response.badRequest(context, "Bad Request, referer is null");
      return;
    }

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
    String fileName = upload.fileName();

    String lastModified = context.request().getHeader("Last-Modified");
    FSUtils.updateFileModifiedDate(fromPath, lastModified);

    DocsService.moveFile(fromPath, fileName, "UPDATE")
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          LogService.addLog("DOC_UPLOAD_FAILED", ip, loginName, fullName, fileName);
          Response.internalError(context, "Upload file failed");
        })
        .compose(v -> {
          return DocsService.addFileInfo(fileName, null);
        })
        .onSuccess(rst -> {
          log.info("Upload file {} success", fileName);
          LogService.addLog("DOC_UPLOAD_SUCCESS", ip, loginName, fullName, fileName);
          Response.success(context, "Upload file success");
        })
        .onFailure(ar -> {
          log.error("{}", ar.getMessage());
          LogService.addLog("DOC_UPLOAD_FAILED", ip, loginName, fullName, fileName);
          Response.internalError(context, "Upload file failed");
        });

  }

}
