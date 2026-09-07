package io.watermelon.ci.gitops.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitOpsProperties.class)
public class GitOpsAutoConfiguration {}
