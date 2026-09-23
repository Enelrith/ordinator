package com.enelrith.ordinator;

import org.springframework.boot.SpringApplication;

public class TestOrdinatorApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrdinatorApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
