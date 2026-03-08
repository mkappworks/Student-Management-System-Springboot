package com.mkappworks.moduleservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.mkappworks.moduleservice", "com.mkappworks.common"})
@EnableDiscoveryClient
public class ModuleServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModuleServiceApplication.class, args);
    }
}
