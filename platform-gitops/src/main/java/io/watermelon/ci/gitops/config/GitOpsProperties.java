package io.watermelon.ci.gitops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "watermelon.gitops")
public class GitOpsProperties {

    private ArgoCd argoCd = new ArgoCd();
    private String gitopsRepoUrl = "http://forgejo:3000/watermelon/gitops.git";
    private String gitopsRepoPath = "/tmp/watermelon-gitops";
    private String defaultCluster = "in-cluster";
    private boolean dryRun = true;

    public ArgoCd getArgoCd() { return argoCd; }
    public void setArgoCd(ArgoCd argoCd) { this.argoCd = argoCd; }
    public String getGitopsRepoUrl() { return gitopsRepoUrl; }
    public void setGitopsRepoUrl(String gitopsRepoUrl) { this.gitopsRepoUrl = gitopsRepoUrl; }
    public String getGitopsRepoPath() { return gitopsRepoPath; }
    public void setGitopsRepoPath(String gitopsRepoPath) { this.gitopsRepoPath = gitopsRepoPath; }
    public String getDefaultCluster() { return defaultCluster; }
    public void setDefaultCluster(String defaultCluster) { this.defaultCluster = defaultCluster; }
    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public static class ArgoCd {
        private String baseUrl = "http://localhost:8089";
        private String token = "changeme";
        private String project = "default";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }
    }
}
