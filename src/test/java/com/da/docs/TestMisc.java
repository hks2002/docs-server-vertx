/*********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                            *
 * @CreatedDate           : 2025-03-21 19:32:00                                                                      *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                            *
 * @LastEditDate          : 2025-07-10 21:19:00                                                                      *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                          *
 ********************************************************************************************************************/

package com.da.docs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestMisc {

  @Test
  public void testColorInfo() {
    log.info("\u001b[35m color");
  }

  @Test
  public void testSqlJson() {
    String text = "Hello #{name}, your code is #{code}. Please check #{file}.";

    // 定义正则表达式
    Pattern pattern = Pattern.compile("#\\{([^}]*)\\}");
    Matcher matcher = pattern.matcher(text);

    // 遍历所有匹配项
    while (matcher.find()) {
      log.info("匹配到的完整文本: " + matcher.group(0)); // 如 #{name}
      log.info("提取的内容: " + matcher.group(1)); // 如 name
    }
  }

  @Test
  public void testSortSet() {
    SortedSet<String> paths = new TreeSet<>();
    paths.add("a");
    paths.add("b");
    paths.add("a/b");
    paths.add("a/b/c");
    paths.add("");

    log.info(paths.toString());
  }

  @Test
  public void testSortFor() {
    List<String> paths = new ArrayList<>();
    paths.add("");
    paths.add("a");
    paths.add("b");
    paths.add("a/b");
    paths.add("a/b/c");

    for (String path : paths) {
      if (path.isEmpty()) {
        log.info("Empty path");
        continue;
      }
      log.info(path);
    }

    for (String path : paths) {
      if (path.equals("b")) {
        break;
      }
      log.info(path);
    }
  }

  @Test
  public void testSortMap() {
    SortedMap<String, String> paths = new TreeMap<>();
    paths.put("a", "a");
    paths.put("b", "b");
    paths.put("a/b", "a/b");
    paths.put("a/b/c", "a/b/c");
    paths.put("", "");

    log.info(paths.toString());
  }

  @Test
  public void testDatetime() {
    long timestamp = 1672531199000L; // 2023-01-01 00:59:59.000
    Instant instant = Instant.ofEpochMilli(timestamp);
    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    String formattedDateTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    log.info(formattedDateTime);
    log.info(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }

  @Test
  public void testListJsonObject() {
    List<JsonObject> msg = new ArrayList<>();
    msg.add(JsonObject.of("v1", "v1"));
    msg.add(JsonObject.of("v2", "v2"));

    log.info(JsonObject.of("success", true, "msg", msg.toArray()).encode());
  }
}
