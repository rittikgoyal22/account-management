package com.etd.account_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AccountManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountManagementApplication.class, args);
	}

}
