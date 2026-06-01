/***********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                              *
 * @CreatedDate           : 2025-03-10 01:05:38                                                                        *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                              *
 * @LastEditDate          : 2026-05-25 09:38:23                                                                        *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                            *
 **********************************************************************************************************************/

package com.da.docs.handler;

import java.util.List;
import java.util.Optional;

import com.da.docs.annotation.Permission;
import com.da.docs.annotation.PostMapping;
import com.da.docs.service.DocsService;
import com.da.docs.service.LogService;
import com.da.docs.serviceStatic.FS;
import com.da.docs.serviceStatic.RESPONSE;
import com.da.docs.utils.CommonUtils;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.internal.net.RFC3986;
import io.vertx.core.json.JsonObject;
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
@Permission("DOCS_WRITE")
public class UploadHandler implements Handler<RoutingContext> {
  private final DocsService docsService = new DocsService();

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    String referer = request.headers().get(HttpHeaders.REFERER);
    String refererPath = RFC3986.normalizePath(referer);

    if (refererPath == null) {
      RESPONSE.badRequest(context, "Bad Request, referer is null");
      return;
    }

    String ip = CommonUtils.getTrueRemoteIp(request);
    String userName = Optional
        .ofNullable(context.user().principal())
        .orElse(new JsonObject())
        .getString("login_name", "none");

    List<FileUpload> uploads = context.fileUploads();
    if (uploads.size() > 1) {
      RESPONSE.badRequest(context, "Bad Request, only one file is allowed");
      return;
    }

    FileUpload upload = uploads.get(0);
    log.trace("{} {} {} {}", upload.uploadedFileName(), upload.fileName(), upload.contentType(), upload.size());
    String fromPath = upload.uploadedFileName().replace('\\', '/');
    String fileName = upload.fileName();

    String lastModified = context.request().getHeader("Last-Modified");
    FS.updateFileModifiedDate(fromPath, lastModified);

    FS.moveFile(fromPath, fileName, "UPDATE")
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
          LogService.addLog("DOC_UPLOAD_FAILED", ip, userName, "", fileName);
          RESPONSE.internalError(context, "Upload file failed");
        })
        .compose(v -> {
          return docsService.addOrModifyFileInfo(fileName, null);
        })
        .onSuccess(rst -> {
          log.info("Upload file {} success", fileName);
          LogService.addLog("DOC_UPLOAD_SUCCESS", ip, userName, "", fileName);
          RESPONSE.success(context, "Upload file success");
        })
        .onFailure(ar -> {
          log.error("{}", ar.getCause());
          LogService.addLog("DOC_UPLOAD_FAILED", ip, userName, "", fileName);
          RESPONSE.internalError(context, "Upload file failed");
        });

  }

}