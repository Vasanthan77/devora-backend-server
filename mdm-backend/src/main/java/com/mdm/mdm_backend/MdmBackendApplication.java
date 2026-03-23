package com.mdm.mdm_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class MdmBackendApplication {

	public static void main(String[] args) {
		// Prevent PostgreSQL startup failures when host timezone alias is unsupported.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(MdmBackendApplication.class, args);
	}

}
