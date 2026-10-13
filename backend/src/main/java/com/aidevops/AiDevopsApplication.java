package com.aidevops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiDevopsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevopsApplication.class, args);
    }
}
