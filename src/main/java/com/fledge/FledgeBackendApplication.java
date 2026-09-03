package com.fledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Scheduled 를 쓰려면 필요하다. 스케줄러 자체는 프로퍼티로 켜고 끈다.
@EnableScheduling
@SpringBootApplication
public class FledgeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FledgeBackendApplication.class, args);
	}

}
