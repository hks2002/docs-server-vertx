package com.da.docs.pojo;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "attachments")
public class Attachments {

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "attachment")
  private List<Attachment> attachments;

}