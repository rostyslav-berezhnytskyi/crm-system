package com.els.crmsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CrmsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrmsystemApplication.class, args);
    }

}
