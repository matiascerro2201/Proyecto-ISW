package com.grupo1.isw.practicas_pedagogicas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PracticasPedagogicasApplication {

	public static void main(String[] args) {
		SpringApplication.run(PracticasPedagogicasApplication.class, args);
	}

}
