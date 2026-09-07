package io.watermelon.ci.secrets.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SecretsProperties.class)
public class SecretsAutoConfiguration {}
