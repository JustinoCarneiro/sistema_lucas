package com.sistema.lucas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync        
@EnableScheduling  
public class LucasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LucasApplication.class, args);
    }
}