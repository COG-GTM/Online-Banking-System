package com.userfront;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.logging.LoggingSystem;

@SpringBootApplication
public class UserFrontApplication {

	public static void main(String[] args) {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		SpringApplication.run(UserFrontApplication.class, args);
	}
}
