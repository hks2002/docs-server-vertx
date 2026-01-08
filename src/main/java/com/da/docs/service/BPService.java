/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-21 15:17:16                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-19 09:34:26                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/

package com.da.docs.service;

import java.util.List;

import com.da.docs.db.DB;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class BPService {

  public Future<List<JsonObject>> searchBPByCode(JsonObject obj) {
    return DB.queryByFile("queryBPByCode0", obj, 0)
        .compose(bpList -> {
          // get from sage and then add to docs db
          if (bpList.size() == 0) {
            return DB.queryByFile("queryBPByCode", obj, 1)
                .onSuccess(ar -> {
                  if (ar.size() == 1) {
                    addBP(ar.get(0));
                  }
                });
            // return from docs db, then get from sage and then update to docs db
          } else if (bpList.size() == 1) {
            return Future.succeededFuture(bpList).andThen(a -> {
              DB.queryByFile("queryBPByCode", obj, 1)
                  .onSuccess(ar -> {
                    if (ar.size() == 1) {
                      updateBP(ar.get(0).put("id", bpList.get(0).getString("id")));
                    }
                  });
            });
          } else {
            return Future.failedFuture("More than one BP found");
          }
        });
  }

  public Future<Object> addBP(JsonObject obj) {
    return DB.insertByFile("insertBP", obj);
  }

  public Future<Object> updateBP(JsonObject obj) {
    return DB.updateByFile("updateBP", obj);
  }

}
