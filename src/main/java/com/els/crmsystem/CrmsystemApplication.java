package com.els.crmsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CrmsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmsystemApplication.class, args);
    }

}
