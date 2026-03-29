package com.chorus;

import com.chorus.config.ChorusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(ChorusProperties.class)
@EnableAsync
public class ChorusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChorusApplication.class, args);
    }
}
