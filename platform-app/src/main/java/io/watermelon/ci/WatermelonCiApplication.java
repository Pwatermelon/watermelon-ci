package io.watermelon.ci;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "io.watermelon.ci")
@EntityScan(basePackages = "io.watermelon.ci.domain")
@EnableJpaRepositories(basePackages = "io.watermelon.ci.domain")
public class WatermelonCiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatermelonCiApplication.class, args);
    }
}
