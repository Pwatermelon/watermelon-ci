package io.watermelon.ci.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "watermelon.demo")
public class DemoProperties {

    /** Soft-fail Jenkins/Vault/Argo and seed sample data for sellable MVP demos. */
    private boolean enabled = true;
    private boolean seedOnStartup = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSeedOnStartup() { return seedOnStartup; }
    public void setSeedOnStartup(boolean seedOnStartup) { this.seedOnStartup = seedOnStartup; }
}
