package uk.nhs.adaptors.gp2gp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
public class Gp2gpApplication {

	public static void main(String[] args) {
		SpringApplication.run(Gp2gpApplication.class, args);
	}

}
