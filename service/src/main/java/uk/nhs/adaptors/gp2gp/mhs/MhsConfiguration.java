package uk.nhs.adaptors.gp2gp.mhs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
public class MhsConfiguration {
    @Value("${gp2gp.mhs.url}")
    private String url;
}
