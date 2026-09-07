package io.watermelon.ci.jenkins.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JenkinsProperties.class)
public class JenkinsAutoConfiguration {}
