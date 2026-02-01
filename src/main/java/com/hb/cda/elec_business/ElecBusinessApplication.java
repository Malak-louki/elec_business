package com.hb.cda.elec_business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElecBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElecBusinessApplication.class, args);
    }

}
