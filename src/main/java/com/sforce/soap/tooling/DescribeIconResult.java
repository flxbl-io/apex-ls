package com.sforce.soap.tooling;

/**
 * This is a generated class for the SObject Enterprise API.
 * Do not edit this file, as your changes will be lost.
 */
public class DescribeIconResult implements com.sforce.ws.bind.XMLizable {

    /**
     * Constructor
     */
    public DescribeIconResult() {}

    /* Cache the typeInfo instead of declaring static fields throughout*/
    private transient java.util.Map<String, com.sforce.ws.bind.TypeInfo> typeInfoCache = new java.util.HashMap<String, com.sforce.ws.bind.TypeInfo>();
    private com.sforce.ws.bind.TypeInfo _lookupTypeInfo(String fieldName, String namespace, String name, String typeNS, String type, int minOcc, int maxOcc, boolean elementForm) {
      com.sforce.ws.bind.TypeInfo typeInfo = typeInfoCache.get(fieldName);
      if (typeInfo == null) {
        typeInfo = new com.sforce.ws.bind.TypeInfo(namespace, name, typeNS, type, minOcc, maxOcc, elementForm);
        typeInfoCache.put(fieldName, typeInfo);
      }
      return typeInfo;
    }

    /**
     * element : contentType of type {http://www.w3.org/2001/XMLSchema}string
     * java type: java.lang.String
     */
    private boolean contentType__is_set = false;

    private java.lang.String contentType;

    public java.lang.String getContentType() {
      return contentType;
    }

    public void setContentType(java.lang.String contentType) {
      this.contentType = contentType;
      contentType__is_set = true;
    }

    protected void setContentType(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.verifyElement(__in, _lookupTypeInfo("contentType", "urn:tooling.soap.sforce.com","contentType","http://www.w3.org/2001/XMLSchema","string",1,1,true))) {
        setContentType(__typeMapper.readString(__in, _lookupTypeInfo("contentType", "urn:tooling.soap.sforce.com","contentType","http://www.w3.org/2001/XMLSchema","string",1,1,true), java.lang.String.class));
      }
    }

    private void writeFieldContentType(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("contentType", "urn:tooling.soap.sforce.com","contentType","http://www.w3.org/2001/XMLSchema","string",1,1,true), contentType, contentType__is_set);
    }

    /**
     * element : height of type {http://www.w3.org/2001/XMLSchema}int
     * java type: java.lang.Integer
     */
    private boolean height__is_set = false;

    private java.lang.Integer height;

    public java.lang.Integer getHeight() {
      return height;
    }

    public void setHeight(java.lang.Integer height) {
      this.height = height;
      height__is_set = true;
    }

    protected void setHeight(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.verifyElement(__in, _lookupTypeInfo("height", "urn:tooling.soap.sforce.com","height","http://www.w3.org/2001/XMLSchema","int",1,1,true))) {
        setHeight((java.lang.Integer)__typeMapper.readObject(__in, _lookupTypeInfo("height", "urn:tooling.soap.sforce.com","height","http://www.w3.org/2001/XMLSchema","int",1,1,true), java.lang.Integer.class));
      }
    }

    private void writeFieldHeight(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("height", "urn:tooling.soap.sforce.com","height","http://www.w3.org/2001/XMLSchema","int",1,1,true), height, height__is_set);
    }

    /**
     * element : theme of type {http://www.w3.org/2001/XMLSchema}string
     * java type: java.lang.String
     */
    private boolean theme__is_set = false;

    private java.lang.String theme;

    public java.lang.String getTheme() {
      return theme;
    }

    public void setTheme(java.lang.String theme) {
      this.theme = theme;
      theme__is_set = true;
    }

    protected void setTheme(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.verifyElement(__in, _lookupTypeInfo("theme", "urn:tooling.soap.sforce.com","theme","http://www.w3.org/2001/XMLSchema","string",1,1,true))) {
        setTheme(__typeMapper.readString(__in, _lookupTypeInfo("theme", "urn:tooling.soap.sforce.com","theme","http://www.w3.org/2001/XMLSchema","string",1,1,true), java.lang.String.class));
      }
    }

    private void writeFieldTheme(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("theme", "urn:tooling.soap.sforce.com","theme","http://www.w3.org/2001/XMLSchema","string",1,1,true), theme, theme__is_set);
    }

    /**
     * element : url of type {http://www.w3.org/2001/XMLSchema}string
     * java type: java.lang.String
     */
    private boolean url__is_set = false;

    private java.lang.String url;

    public java.lang.String getUrl() {
      return url;
    }

    public void setUrl(java.lang.String url) {
      this.url = url;
      url__is_set = true;
    }

    protected void setUrl(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.verifyElement(__in, _lookupTypeInfo("url", "urn:tooling.soap.sforce.com","url","http://www.w3.org/2001/XMLSchema","string",1,1,true))) {
        setUrl(__typeMapper.readString(__in, _lookupTypeInfo("url", "urn:tooling.soap.sforce.com","url","http://www.w3.org/2001/XMLSchema","string",1,1,true), java.lang.String.class));
      }
    }

    private void writeFieldUrl(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("url", "urn:tooling.soap.sforce.com","url","http://www.w3.org/2001/XMLSchema","string",1,1,true), url, url__is_set);
    }

    /**
     * element : width of type {http://www.w3.org/2001/XMLSchema}int
     * java type: java.lang.Integer
     */
    private boolean width__is_set = false;

    private java.lang.Integer width;

    public java.lang.Integer getWidth() {
      return width;
    }

    public void setWidth(java.lang.Integer width) {
      this.width = width;
      width__is_set = true;
    }

    protected void setWidth(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.verifyElement(__in, _lookupTypeInfo("width", "urn:tooling.soap.sforce.com","width","http://www.w3.org/2001/XMLSchema","int",1,1,true))) {
        setWidth((java.lang.Integer)__typeMapper.readObject(__in, _lookupTypeInfo("width", "urn:tooling.soap.sforce.com","width","http://www.w3.org/2001/XMLSchema","int",1,1,true), java.lang.Integer.class));
      }
    }

    private void writeFieldWidth(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("width", "urn:tooling.soap.sforce.com","width","http://www.w3.org/2001/XMLSchema","int",1,1,true), width, width__is_set);
    }

    /**
     */
    @Override
    public void write(javax.xml.namespace.QName __element,
        com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper)
        throws java.io.IOException {
      __out.writeStartTag(__element.getNamespaceURI(), __element.getLocalPart());
      writeFields(__out, __typeMapper);
      __out.writeEndTag(__element.getNamespaceURI(), __element.getLocalPart());
    }

    protected void writeFields(com.sforce.ws.parser.XmlOutputStream __out,
         com.sforce.ws.bind.TypeMapper __typeMapper)
         throws java.io.IOException {
       writeFields1(__out, __typeMapper);
    }

    @Override
    public void load(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __typeMapper.consumeStartTag(__in);
      loadFields(__in, __typeMapper);
      __typeMapper.consumeEndTag(__in);
    }

    protected void loadFields(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
        loadFields1(__in, __typeMapper);
    }

    @Override
    public String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder();
      sb.append("[DescribeIconResult ");
      toString1(sb);

      sb.append("]\n");
      return sb.toString();
    }

    private void toStringHelper(StringBuilder sb, String name, Object value) {
      sb.append(' ').append(name).append("='").append(com.sforce.ws.util.Verbose.toString(value)).append("'\n");
    }

    private void writeFields1(com.sforce.ws.parser.XmlOutputStream __out,
         com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      writeFieldContentType(__out, __typeMapper);
      writeFieldHeight(__out, __typeMapper);
      writeFieldTheme(__out, __typeMapper);
      writeFieldUrl(__out, __typeMapper);
      writeFieldWidth(__out, __typeMapper);
    }

    private void loadFields1(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      setContentType(__in, __typeMapper);
      setHeight(__in, __typeMapper);
      setTheme(__in, __typeMapper);
      setUrl(__in, __typeMapper);
      setWidth(__in, __typeMapper);
    }

    private void toString1(StringBuilder sb) {
      toStringHelper(sb, "contentType", contentType);
      toStringHelper(sb, "height", height);
      toStringHelper(sb, "theme", theme);
      toStringHelper(sb, "url", url);
      toStringHelper(sb, "width", width);
    }


}