package com.bank.bootcamp.credits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class CreditsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditsApplication.class, args);
	}

}
