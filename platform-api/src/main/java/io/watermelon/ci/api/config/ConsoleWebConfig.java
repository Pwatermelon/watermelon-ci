package io.watermelon.ci.api.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class ConsoleWebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/app").setViewName("forward:/index.html");
        registry.addViewController("/app/").setViewName("forward:/index.html");
        registry.addViewController("/docs").setViewName("forward:/index.html");
        registry.addViewController("/docs/").setViewName("forward:/index.html");
        registry.addViewController("/docs/karbyz").setViewName("forward:/index.html");
        registry.addViewController("/app/karbyz").setViewName("forward:/index.html");
        registry.addViewController("/examples/karbyz").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");

        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/");

        // SPA routes
        registry.addResourceHandler(
                        "/",
                        "/app",
                        "/app/",
                        "/app/**",
                        "/docs",
                        "/docs/",
                        "/docs/**",
                        "/examples",
                        "/examples/",
                        "/examples/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
