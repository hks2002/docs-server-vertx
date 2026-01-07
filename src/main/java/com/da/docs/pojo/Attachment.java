package com.da.docs.pojo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JacksonXmlRootElement(localName = "attachment")
public class Attachment {

  @JacksonXmlProperty(localName = "id")
  private long id;

  @JacksonXmlProperty(localName = "name")
  private String name;

  @JacksonXmlProperty(localName = "format")
  private String format;

  @JacksonXmlProperty(localName = "size")
  private long size;

  @JacksonXmlProperty(localName = "archive")
  private boolean archive;

  @JacksonXmlProperty(localName = "backup")
  private boolean backup;

  @JacksonXmlProperty(localName = "replicated")
  private boolean replicated;

  @JacksonXmlProperty(localName = "repl_site")
  private String replSite;

  @JacksonXmlProperty(localName = "site_ok")
  private boolean siteOk;

  @JacksonXmlProperty(localName = "d_ou_f")
  private String dOuF;

  @JacksonXmlProperty(localName = "date_modif")
  private String dateModif;

  @JacksonXmlProperty(localName = "comm")
  private String comm;

}