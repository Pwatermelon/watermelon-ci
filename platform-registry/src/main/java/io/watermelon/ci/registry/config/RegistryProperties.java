package io.watermelon.ci.registry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermelon.registry")
public class RegistryProperties {

    private Docker docker = new Docker();
    private Maven maven = new Maven();

    public Docker getDocker() { return docker; }
    public void setDocker(Docker docker) { this.docker = docker; }
    public Maven getMaven() { return maven; }
    public void setMaven(Maven maven) { this.maven = maven; }

    public static class Docker {
        /** Harbor or distribution registry API base, e.g. https://harbor.local */
        private String baseUrl = "http://localhost:5000";
        private String project = "watermelon";
        private String username = "admin";
        private String password = "Harbor12345";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String pushHost() {
            String host = baseUrl.replace("https://", "").replace("http://", "");
            if (host.endsWith("/")) {
                host = host.substring(0, host.length() - 1);
            }
            return host + "/" + project;
        }
    }

    public static class Maven {
        /** Nexus / Artifactory Maven repository URL */
        private String repositoryUrl = "http://localhost:8081/repository/maven-releases/";
        private String username = "admin";
        private String password = "admin123";

        public String getRepositoryUrl() { return repositoryUrl; }
        public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
