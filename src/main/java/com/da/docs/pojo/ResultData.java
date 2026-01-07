package com.da.docs.pojo;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "resultdata")
public class ResultData {
  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "result")
  private List<Result> results;
}
