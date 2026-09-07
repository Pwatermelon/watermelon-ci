package io.watermelon.ci.git.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GitProperties.class)
public class GitAutoConfiguration {}
