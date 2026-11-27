package com.mealbooking.backend;

import com.mealbooking.backend.config.LocalDatabaseBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MealBookingApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MealBookingApplication.class);
        application.addInitializers(new LocalDatabaseBootstrap());
        application.run(args);
    }
}
