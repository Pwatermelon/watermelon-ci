package io.watermelon.ci.runtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermelon.runtime")
public class RuntimeProperties {

    private Docker docker = new Docker();
    private long pollIntervalSeconds = 30;

    public Docker getDocker() { return docker; }
    public void setDocker(Docker docker) { this.docker = docker; }
    public long getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(long pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }

    public static class Docker {
        /** Docker Engine API, e.g. unix:///var/run/docker.sock proxied as http://localhost:2375 */
        private String engineUrl = "http://localhost:2375";
        private boolean enabled = true;

        public String getEngineUrl() { return engineUrl; }
        public void setEngineUrl(String engineUrl) { this.engineUrl = engineUrl; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
