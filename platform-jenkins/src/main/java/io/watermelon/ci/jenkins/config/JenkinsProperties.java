package io.watermelon.ci.jenkins.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermelon.jenkins")
public class JenkinsProperties {

    private String baseUrl = "http://localhost:8080";
    private String username = "admin";
    private String apiToken = "changeme";
    private String crumbIssuerPath = "/crumbIssuer/api/json";
    /** When true, Jenkins API failures are logged and ignored (MVP demo mode). */
    private boolean softFail = true;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public String getCrumbIssuerPath() { return crumbIssuerPath; }
    public void setCrumbIssuerPath(String crumbIssuerPath) { this.crumbIssuerPath = crumbIssuerPath; }
    public boolean isSoftFail() { return softFail; }
    public void setSoftFail(boolean softFail) { this.softFail = softFail; }
}
