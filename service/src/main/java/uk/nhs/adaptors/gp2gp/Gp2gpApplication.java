package uk.nhs.adaptors.gp2gp;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Gp2gpApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Gp2gpApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

}
