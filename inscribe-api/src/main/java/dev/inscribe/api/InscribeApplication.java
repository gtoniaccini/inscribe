package dev.inscribe.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "dev.inscribe")
@EntityScan(basePackages = "dev.inscribe")
@EnableJpaRepositories(basePackages = "dev.inscribe")
@EnableScheduling
public class InscribeApplication {

    public static void main(String[] args) {
        SpringApplication.run(InscribeApplication.class, args);
    }
}
