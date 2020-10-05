package uk.nhs.adaptors.gp2gp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@SpringBootApplication
public class GP2GPApplication {

	public static void main(String[] args) {
		SpringApplication.run(GP2GPApplication.class, args);
	}

}
