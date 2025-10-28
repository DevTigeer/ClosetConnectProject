package com.tigger.closetconnectproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class ClosetConnectProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClosetConnectProjectApplication.class, args);
    }

}
