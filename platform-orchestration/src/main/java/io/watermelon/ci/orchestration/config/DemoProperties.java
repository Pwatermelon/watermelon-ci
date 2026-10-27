package io.watermelon.ci.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "watermelon.demo")
public class DemoProperties {

    /** Soft-fail Jenkins/Vault/Argo and seed sample data for sellable MVP demos. */
    private boolean enabled = true;
    private boolean seedOnStartup = true;
    /** After seed, pull/run sample apps onto demo VMs. */
    private boolean seedRuntime = true;
    private String vmAUrl = "http://demo-vm-a:2375";
    private String vmBUrl = "http://demo-vm-b:2375";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSeedOnStartup() { return seedOnStartup; }
    public void setSeedOnStartup(boolean seedOnStartup) { this.seedOnStartup = seedOnStartup; }
    public boolean isSeedRuntime() { return seedRuntime; }
    public void setSeedRuntime(boolean seedRuntime) { this.seedRuntime = seedRuntime; }
    public String getVmAUrl() { return vmAUrl; }
    public void setVmAUrl(String vmAUrl) { this.vmAUrl = vmAUrl; }
    public String getVmBUrl() { return vmBUrl; }
    public void setVmBUrl(String vmBUrl) { this.vmBUrl = vmBUrl; }
}
