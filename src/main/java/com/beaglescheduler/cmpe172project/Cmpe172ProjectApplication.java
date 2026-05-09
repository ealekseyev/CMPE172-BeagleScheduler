package com.beaglescheduler.cmpe172project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Cmpe172ProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cmpe172ProjectApplication.class, args);
    }

}
