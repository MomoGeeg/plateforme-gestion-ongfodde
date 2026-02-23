package ong.fodde;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import ong.fodde.config.JwtConfig;

@SpringBootApplication
@EnableScheduling 
@EnableConfigurationProperties(JwtConfig.class)
public class OngfoddeApplication {
    public static void main(String[] args) {
        SpringApplication.run(OngfoddeApplication.class, args);
    }
}