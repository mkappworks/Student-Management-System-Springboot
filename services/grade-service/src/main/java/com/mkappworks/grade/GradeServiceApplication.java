package com.mkappworks.grade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.mkappworks.grade", "com.mkappworks.common"})
@EnableDiscoveryClient
public class GradeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradeServiceApplication.class, args);
    }
}
