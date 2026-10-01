package com.userfront;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration;

@SpringBootApplication(exclude = SpringDataWebAutoConfiguration.class)
public class UserFrontApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserFrontApplication.class, args);
	}
}
