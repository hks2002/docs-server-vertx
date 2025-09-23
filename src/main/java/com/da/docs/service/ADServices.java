/**********************************************************************************************************************
 * @Author                : Robert Huang<56649783@qq.com>                                                             *
 * @CreatedDate           : 2025-03-28 00:03:05                                                                       *
 * @LastEditors           : Robert Huang<56649783@qq.com>                                                             *
 * @LastEditDate          : 2025-09-19 00:07:16                                                                       *
 * @CopyRight             : Dedienne Aerospace China ZhuHai                                                           *
 *********************************************************************************************************************/


package com.da.docs.service;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.da.docs.config.DocsConfig;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ADServices {

  /**
   * Authenticates a user against an Active Directory server and retrieves user
   * information.
   *
   * @param username The username to authenticate
   * @param password The password for authentication
   * @return A Future containing a JsonObject with user details (login_name,
   *         first_name, last_name, email, full_name)
   *         if authentication succeeds, or a failed Future if authentication
   *         fails
   */
  public Future<JsonObject> adAuthorization(
      String username,
      String password) {
    JsonObject adConfig = DocsConfig.handleConfig.getJsonObject("adServer", new JsonObject());
    String adServerUrl = adConfig.getString("url");
    String adServerDomain = adConfig.getString("domain");
    String searchBase = adConfig.getString("searchBase");

    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.PROVIDER_URL, adServerUrl);
    env.put(Context.SECURITY_PRINCIPAL, username + "@" + adServerDomain);
    env.put(Context.SECURITY_CREDENTIALS, password);

    // Skip SSL verification if using LDAPS
    // hostname verification still works, so must using a valid full hostname
    if (adServerUrl.toLowerCase().startsWith("ldaps")) {
      env.put("java.naming.ldap.factory.socket", "com.da.docs.ssl.TrustAllSSLSocketFactory");
    }

    DirContext dirCtx = null;
    JsonObject user = null;

    try {
      dirCtx = new InitialDirContext(env);

      SearchControls searchControls = new SearchControls();
      searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      searchControls.setCountLimit(1);
      searchControls.setTimeLimit(10000);
      String searchFilter = "(&(objectCategory=person)(objectClass=user)(sAMAccountName=" + username + "))";

      NamingEnumeration<SearchResult> results = dirCtx.search(searchBase, searchFilter, searchControls);

      if (results.hasMore()) {
        SearchResult searchResult = results.next();
        Attributes attributes = searchResult.getAttributes();

        // [some attributes]:
        // name(cn)
        // givenName
        // sn
        // sAMAccountName
        // mail
        // memberOf
        // whenCreated
        // whenChanged

        Attribute loginName = attributes.get("sAMAccountName");
        Attribute sn = attributes.get("sn");
        Attribute givenName = attributes.get("givenName");
        Attribute mail = attributes.get("mail");
        // Attribute memberOf = attributes.get("memberOf");
        // Attribute whenCreated = attributes.get("whenCreated");
        // Attribute whenChanged = attributes.get("whenChanged");

        user = new JsonObject();
        user.put("login_name", loginName == null ? "" : loginName.toString().split(": ")[1]);
        user.put("first_name", givenName == null ? "" : givenName.toString().split(": ")[1]);
        user.put("last_name", sn == null ? "" : sn.toString().split(": ")[1]);
        user.put("email", mail == null ? "" : mail.toString().split(": ")[1]);
        user.put("full_name", user.getString("first_name") + " " + user.getString("last_name"));
      }
    } catch (AuthenticationException e) {
      log.error("AuthenticationException: {} {}", e.getMessage(), e.getRootCause());
    } catch (NamingException e) {
      log.error("NamingException: {} {}", e.getMessage(), e.getRootCause());
    } finally {
      if (dirCtx != null) {
        try {
          dirCtx.close();
        } catch (NamingException e) {
          log.error("{}", e);
        }
      }
    }

    // return user, it may be null
    return user == null ? Future.failedFuture("Authentication Failed") : Future.succeededFuture(user);
  }
}
