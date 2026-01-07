package com.da.docs.pojo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "result")
public class Result {
  @JacksonXmlProperty(localName = "result_name")
  private String resultName;

  @JacksonXmlProperty(localName = "result_value")
  private boolean resultValue;

  @JacksonXmlProperty(localName = "result_code")
  private int resultCode;

  @JacksonXmlProperty(localName = "result_detail")
  private String resultDetail;
}
