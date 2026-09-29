package io.watermelon.ci.secrets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermelon.secrets")
public class SecretsProperties {

    /** OpenBao / HashiCorp Vault HTTP API */
    private String vaultAddr = "http://localhost:8200";
    private String token = "root";
    private String kvMount = "secret";
    private String pathPrefix = "watermelon";
    /** Fallback to in-memory store when Vault is unreachable (MVP demos). */
    private boolean memoryFallback = true;

    public String getVaultAddr() { return vaultAddr; }
    public void setVaultAddr(String vaultAddr) { this.vaultAddr = vaultAddr; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getKvMount() { return kvMount; }
    public void setKvMount(String kvMount) { this.kvMount = kvMount; }
    public String getPathPrefix() { return pathPrefix; }
    public void setPathPrefix(String pathPrefix) { this.pathPrefix = pathPrefix; }
    public boolean isMemoryFallback() { return memoryFallback; }
    public void setMemoryFallback(boolean memoryFallback) { this.memoryFallback = memoryFallback; }
}
