package com.sforce.soap.tooling.metadata;

/**
 * This is a generated class for the SObject Enterprise API.
 * Do not edit this file, as your changes will be lost.
 */
public class SecuritySettings extends com.sforce.soap.tooling.metadata.MetadataForSettings {

    /**
     * Constructor
     */
    public SecuritySettings() {}

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
     * element : networkAccess of type {urn:metadata.tooling.soap.sforce.com}NetworkAccess
     * java type: com.sforce.soap.tooling.metadata.NetworkAccess
     */
    private boolean networkAccess__is_set = false;

    private com.sforce.soap.tooling.metadata.NetworkAccess networkAccess;

    public com.sforce.soap.tooling.metadata.NetworkAccess getNetworkAccess() {
      return networkAccess;
    }

    public void setNetworkAccess(com.sforce.soap.tooling.metadata.NetworkAccess networkAccess) {
      this.networkAccess = networkAccess;
      networkAccess__is_set = true;
    }

    protected void setNetworkAccess(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.isElement(__in, _lookupTypeInfo("networkAccess", "urn:metadata.tooling.soap.sforce.com","networkAccess","urn:metadata.tooling.soap.sforce.com","NetworkAccess",0,1,true))) {
        setNetworkAccess((com.sforce.soap.tooling.metadata.NetworkAccess)__typeMapper.readObject(__in, _lookupTypeInfo("networkAccess", "urn:metadata.tooling.soap.sforce.com","networkAccess","urn:metadata.tooling.soap.sforce.com","NetworkAccess",0,1,true), com.sforce.soap.tooling.metadata.NetworkAccess.class));
      }
    }

    private void writeFieldNetworkAccess(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("networkAccess", "urn:metadata.tooling.soap.sforce.com","networkAccess","urn:metadata.tooling.soap.sforce.com","NetworkAccess",0,1,true), networkAccess, networkAccess__is_set);
    }

    /**
     * element : passwordPolicies of type {urn:metadata.tooling.soap.sforce.com}PasswordPolicies
     * java type: com.sforce.soap.tooling.metadata.PasswordPolicies
     */
    private boolean passwordPolicies__is_set = false;

    private com.sforce.soap.tooling.metadata.PasswordPolicies passwordPolicies;

    public com.sforce.soap.tooling.metadata.PasswordPolicies getPasswordPolicies() {
      return passwordPolicies;
    }

    public void setPasswordPolicies(com.sforce.soap.tooling.metadata.PasswordPolicies passwordPolicies) {
      this.passwordPolicies = passwordPolicies;
      passwordPolicies__is_set = true;
    }

    protected void setPasswordPolicies(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.isElement(__in, _lookupTypeInfo("passwordPolicies", "urn:metadata.tooling.soap.sforce.com","passwordPolicies","urn:metadata.tooling.soap.sforce.com","PasswordPolicies",0,1,true))) {
        setPasswordPolicies((com.sforce.soap.tooling.metadata.PasswordPolicies)__typeMapper.readObject(__in, _lookupTypeInfo("passwordPolicies", "urn:metadata.tooling.soap.sforce.com","passwordPolicies","urn:metadata.tooling.soap.sforce.com","PasswordPolicies",0,1,true), com.sforce.soap.tooling.metadata.PasswordPolicies.class));
      }
    }

    private void writeFieldPasswordPolicies(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("passwordPolicies", "urn:metadata.tooling.soap.sforce.com","passwordPolicies","urn:metadata.tooling.soap.sforce.com","PasswordPolicies",0,1,true), passwordPolicies, passwordPolicies__is_set);
    }

    /**
     * element : sessionSettings of type {urn:metadata.tooling.soap.sforce.com}SessionSettings
     * java type: com.sforce.soap.tooling.metadata.SessionSettings
     */
    private boolean sessionSettings__is_set = false;

    private com.sforce.soap.tooling.metadata.SessionSettings sessionSettings;

    public com.sforce.soap.tooling.metadata.SessionSettings getSessionSettings() {
      return sessionSettings;
    }

    public void setSessionSettings(com.sforce.soap.tooling.metadata.SessionSettings sessionSettings) {
      this.sessionSettings = sessionSettings;
      sessionSettings__is_set = true;
    }

    protected void setSessionSettings(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      __in.peekTag();
      if (__typeMapper.isElement(__in, _lookupTypeInfo("sessionSettings", "urn:metadata.tooling.soap.sforce.com","sessionSettings","urn:metadata.tooling.soap.sforce.com","SessionSettings",0,1,true))) {
        setSessionSettings((com.sforce.soap.tooling.metadata.SessionSettings)__typeMapper.readObject(__in, _lookupTypeInfo("sessionSettings", "urn:metadata.tooling.soap.sforce.com","sessionSettings","urn:metadata.tooling.soap.sforce.com","SessionSettings",0,1,true), com.sforce.soap.tooling.metadata.SessionSettings.class));
      }
    }

    private void writeFieldSessionSettings(com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      __typeMapper.writeObject(__out, _lookupTypeInfo("sessionSettings", "urn:metadata.tooling.soap.sforce.com","sessionSettings","urn:metadata.tooling.soap.sforce.com","SessionSettings",0,1,true), sessionSettings, sessionSettings__is_set);
    }

    /**
     */
    @Override
    public void write(javax.xml.namespace.QName __element,
        com.sforce.ws.parser.XmlOutputStream __out, com.sforce.ws.bind.TypeMapper __typeMapper)
        throws java.io.IOException {
      __out.writeStartTag(__element.getNamespaceURI(), __element.getLocalPart());
      __typeMapper.writeXsiType(__out, "urn:metadata.tooling.soap.sforce.com", "SecuritySettings");
      writeFields(__out, __typeMapper);
      __out.writeEndTag(__element.getNamespaceURI(), __element.getLocalPart());
    }

    protected void writeFields(com.sforce.ws.parser.XmlOutputStream __out,
         com.sforce.ws.bind.TypeMapper __typeMapper)
         throws java.io.IOException {
       super.writeFields(__out, __typeMapper);
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
        super.loadFields(__in, __typeMapper);
        loadFields1(__in, __typeMapper);
    }

    @Override
    public String toString() {
      java.lang.StringBuilder sb = new java.lang.StringBuilder();
      sb.append("[SecuritySettings ");
      sb.append(super.toString());
      toString1(sb);

      sb.append("]\n");
      return sb.toString();
    }

    private void toStringHelper(StringBuilder sb, String name, Object value) {
      sb.append(' ').append(name).append("='").append(com.sforce.ws.util.Verbose.toString(value)).append("'\n");
    }

    private void writeFields1(com.sforce.ws.parser.XmlOutputStream __out,
         com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException {
      writeFieldNetworkAccess(__out, __typeMapper);
      writeFieldPasswordPolicies(__out, __typeMapper);
      writeFieldSessionSettings(__out, __typeMapper);
    }

    private void loadFields1(com.sforce.ws.parser.XmlInputStream __in,
        com.sforce.ws.bind.TypeMapper __typeMapper) throws java.io.IOException, com.sforce.ws.ConnectionException {
      setNetworkAccess(__in, __typeMapper);
      setPasswordPolicies(__in, __typeMapper);
      setSessionSettings(__in, __typeMapper);
    }

    private void toString1(StringBuilder sb) {
      toStringHelper(sb, "networkAccess", networkAccess);
      toStringHelper(sb, "passwordPolicies", passwordPolicies);
      toStringHelper(sb, "sessionSettings", sessionSettings);
    }


}