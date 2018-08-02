package org.glygen.array.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public class TripleStoreProperties implements InitializingBean {
  public static final String TS_CONFIGURATION_PREFIX = "spring.triplestore";

  @Value("${" + TS_CONFIGURATION_PREFIX + ".driverClassName:virtuoso.jdbc4.Driver}")
  private String driverClassName;

  @Value("${" + TS_CONFIGURATION_PREFIX + ".url:jdbc:virtuoso://127.0.0.1:1111}")
  private String url;

  @Value("${" + TS_CONFIGURATION_PREFIX + ".username:dba}")
  private String username;

  @Value("${" + TS_CONFIGURATION_PREFIX + ".password:dba}")
  private String password;

  private ClassLoader classLoader;

  private boolean initialize = true;

  private String platform = "all";

  private String schema;

  private String data;

  private boolean continueOnError = false;

  private String separator = ";";

  private String sqlScriptEncoding;

  public String getDriverClassName() {
    return driverClassName;
  }

  public String getUrl() {
    return this.url;
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public void setDriverClassName(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isInitialize() {
    return this.initialize;
  }

  public void setInitialize(boolean initialize) {
    this.initialize = initialize;
  }

  public String getPlatform() {
    return this.platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getSchema() {
    return this.schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getData() {
    return this.data;
  }

  public void setData(String script) {
    this.data = script;
  }

  public boolean isContinueOnError() {
    return this.continueOnError;
  }

  public void setContinueOnError(boolean continueOnError) {
    this.continueOnError = continueOnError;
  }

  public String getSeparator() {
    return this.separator;
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

  public String getSqlScriptEncoding() {
    return sqlScriptEncoding;
  }

  public void setSqlScriptEncoding(String sqlScriptEncoding) {
    this.sqlScriptEncoding = sqlScriptEncoding;
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  /**
   * Checks mandatory properties
   *
   * @see InitializingBean#afterPropertiesSet()
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.state(url != null, "A url is required");
    Assert.state(username != null, "The username is required");
    Assert.state(StringUtils.hasText(password), "A password is required");
    // Assert.state(StringUtils.hasText(orderBy),
    // "A ORDER BY statement is required");
  }
}
