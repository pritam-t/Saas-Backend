package com.pritam.saasbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SaasBackendApplication {

	public static void main(String[] args) {

		System.out.println(
				"JWT_SECRET present: "
						+ (System.getenv("JWT_SECRET") != null)
		);

		SpringApplication.run(SaasBackendApplication.class, args);
	}

}
