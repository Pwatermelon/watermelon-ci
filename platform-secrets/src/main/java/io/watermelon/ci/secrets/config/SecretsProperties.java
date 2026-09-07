package io.watermelon.ci.secrets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermelon.secrets")
public class SecretsProperties {

    /** OpenBao / HashiCorp Vault HTTP API */
    private String vaultAddr = "http://localhost:8200";
    private String token = "root";
    private String kvMount = "secret";
    private String pathPrefix = "watermelon";

    public String getVaultAddr() { return vaultAddr; }
    public void setVaultAddr(String vaultAddr) { this.vaultAddr = vaultAddr; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getKvMount() { return kvMount; }
    public void setKvMount(String kvMount) { this.kvMount = kvMount; }
    public String getPathPrefix() { return pathPrefix; }
    public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }
}
