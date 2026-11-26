package com.cotizador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CotizadorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CotizadorApplication.class, args);
	}

}
