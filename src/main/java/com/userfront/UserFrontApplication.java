package com.userfront;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LoggingSystem;

@SpringBootApplication
public class UserFrontApplication {

	public static void main(String[] args) {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
		SpringApplication.run(UserFrontApplication.class, args);
	}
}
