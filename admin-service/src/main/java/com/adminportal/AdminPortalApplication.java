package com.adminportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.adminportal", "com.userfront"})
@EntityScan("com.userfront.domain")
@EnableJpaRepositories("com.userfront.dao")
public class AdminPortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminPortalApplication.class, args);
	}
}
